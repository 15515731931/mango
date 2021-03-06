/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.partition;

/**
 * 数据源路由
 *
 * @author ash
 */
public interface DataSourceRouter<T> {

    /**
     * 获得数据源名称
     *
     * @param shardParam
     *          取{@link org.jfaster.mango.annotation.ShardBy}修饰的参数
     *
     * @param type
     *          取{@link org.jfaster.mango.annotation.ShardBy#type()}中的值
     *
     * @return
     */
    public String getDataSourceName(T shardParam, int type);

}
