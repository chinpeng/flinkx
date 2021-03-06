/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.writer;

import com.dtstack.flinkx.config.DataTransferConfig;
import com.dtstack.flinkx.config.DirtyConfig;
import com.dtstack.flinkx.plugin.PluginLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.types.Row;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract specification of Writer Plugin
 *
 * Company: www.dtstack.com
 * @author huyifan.zju@163.com
 */
public abstract class DataWriter {

    protected String mode;

    protected String monitorUrls;

    protected PluginLoader pluginLoader;

    protected int errors;

    protected double errorRatio;

    protected String dirtyPath;

    protected Map<String,String> dirtyHadoopConfig;

    protected List<String> srcCols = new ArrayList<>();

    public List<String> getSrcCols() {
        return srcCols;
    }

    public void setSrcCols(List<String> srcCols) {
        this.srcCols = srcCols;
    }

    public PluginLoader getPluginLoader() {
        return pluginLoader;
    }

    public void setPluginLoader(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    public DataWriter(DataTransferConfig config) {
        this.monitorUrls = config.getMonitorUrls();
        this.errors = config.getJob().getSetting().getErrorLimit().getRecord();
        double percentage = config.getJob().getSetting().getErrorLimit().getPercentage();
        this.errorRatio = percentage / 100.0;
        DirtyConfig dirtyConfig =  config.getJob().getSetting().getDirty();
        if(dirtyConfig != null) {
            String dirtyPath = dirtyConfig.getPath();
            Map<String,String> dirtyHadoopConfig = dirtyConfig.getHadoopConfig();
            if(dirtyPath != null) {
                this.dirtyPath = dirtyPath;
            }
            if(dirtyHadoopConfig != null) {
                this.dirtyHadoopConfig = dirtyHadoopConfig;
            }
        }

        List columns = config.getJob().getContent().get(0).getReader().getParameter().getColumn();

        if(columns == null || columns.size() == 0) {
            throw new RuntimeException("source columns can't be null or empty");
        }

        System.out.println("src class: " + columns.get(0).getClass());

        if(columns.get(0) instanceof String) {
            for(Object column : columns) {
                srcCols.add((String)column);
            }
        } else if(columns.get(0) instanceof Map) {
            this.srcCols = new ArrayList<>();
            for(Object column : columns) {
                Map<String,Object> colMap = (Map<String,Object>) column;
                String colName = (String) colMap.get("name");
                if(StringUtils.isBlank(colName)) {
                    Object colIndex = colMap.get("index");
                    if(colIndex != null) {
                        if(colIndex instanceof Integer) {
                            colName = String.valueOf(colIndex);
                        } else if(colIndex instanceof Double) {
                            Double doubleColIndex = (Double) colIndex;
                            colName = String.valueOf(doubleColIndex.intValue());
                        } else {
                            throw new RuntimeException("invalid src col index");
                        }
                    } else {
                        String colValue = (String) colMap.get("value");
                        if(StringUtils.isNotBlank(colValue)) {
                            colName = "val_" +colValue;
                        } else {
                            throw new RuntimeException("can't determine source column name");
                        }
                    }
                }
                srcCols.add(colName);
            }
        }

    }

    public abstract DataStreamSink<?> writeData(DataStream<Row> dataSet);

}
