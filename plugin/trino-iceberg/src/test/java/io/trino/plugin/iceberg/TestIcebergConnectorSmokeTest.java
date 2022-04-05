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
package io.trino.plugin.iceberg;

import io.trino.testing.QueryRunner;

import static org.apache.iceberg.FileFormat.ORC;

// Redundant over TestIcebergOrcConnectorTest, but exists to exercise BaseConnectorSmokeTest
// Some features like materialized views may be supported by Iceberg only.
public class TestIcebergConnectorSmokeTest
        extends BaseIcebergConnectorSmokeTest
{
    public TestIcebergConnectorSmokeTest()
    {
        super(ORC);
    }

    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        return IcebergQueryRunner.builder()
                .setInitialTables(REQUIRED_TPCH_TABLES)
                .build();
    }

    @Override
    protected String getMetadataDirectory(String tableName)
    {
        return getDistributedQueryRunner().getCoordinator().getBaseDataDir()
                .resolve("iceberg_data")
                .resolve(getSession().getSchema().orElseThrow())
                .resolve(tableName)
                .resolve("metadata")
                .toFile()
                .getPath();
    }
}
