/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.exchange;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import jakarta.annotation.Nullable;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Futures.immediateVoidFuture;

@ThreadSafe
public class LocalExchangeMemoryManager
{
    private static final ListenableFuture<Void> NOT_BLOCKED = immediateVoidFuture();

    private final long maxBufferedBytes;
    private final AtomicLong bufferedBytes = new AtomicLong();

    @Nullable
    @GuardedBy("this")
    private SettableFuture<Void> notFullFuture; // null represents "no callback registered"

    public LocalExchangeMemoryManager(long maxBufferedBytes)
    {
        checkArgument(maxBufferedBytes > 0, "maxBufferedBytes must be > 0");
        this.maxBufferedBytes = maxBufferedBytes;
    }

    public void updateMemoryUsage(long bytesAdded)
    {
        long bufferedBytes = this.bufferedBytes.addAndGet(bytesAdded);
        // detect the transition from above to below the full boundary
        if (bufferedBytes <= maxBufferedBytes && (bufferedBytes - bytesAdded) > maxBufferedBytes) {
            SettableFuture<Void> future;
            synchronized (this) {
                // if we have no callback waiting, return early
                if (notFullFuture == null) {
                    return;
                }
                future = notFullFuture;
                notFullFuture = null;
            }
            // complete future outside of lock since this can invoke callbacks
            future.set(null);
        }
    }

    public ListenableFuture<Void> getNotFullFuture()
    {
        if (bufferedBytes.get() <= maxBufferedBytes) {
            return NOT_BLOCKED;
        }
        synchronized (this) {
            // Recheck after synchronizing but before creating a real listener
            if (bufferedBytes.get() <= maxBufferedBytes) {
                return NOT_BLOCKED;
            }
            // if we are full and no current listener is registered, create one
            if (notFullFuture == null) {
                notFullFuture = SettableFuture.create();
            }
            return notFullFuture;
        }
    }

    public long getBufferedBytes()
    {
        return bufferedBytes.get();
    }
}
