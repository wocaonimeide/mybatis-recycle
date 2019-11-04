/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder;

import io.netty.util.Recycler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.recyle.Recyle;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * @author Clinton Begin
 */
public class StaticSqlSource implements SqlSource, Recyle {

  private static final Recycler<StaticSqlSource> RECYCLER=new Recycler<StaticSqlSource>() {
    @Override
    protected StaticSqlSource newObject(Handle<StaticSqlSource> handle) {
      return new StaticSqlSource(handle);
    }
  };

  private Recycler.Handle<StaticSqlSource> handle;
  private String sql;
  private List<ParameterMapping> parameterMappings;
  private Configuration configuration;

  private StaticSqlSource(Recycler.Handle<StaticSqlSource> handle){
    this.handle=handle;
  }

  public static StaticSqlSource newInstance(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    StaticSqlSource staticSqlSource=RECYCLER.get();
    staticSqlSource.initSqlSource(configuration, sql,parameterMappings);
    return staticSqlSource;
  }

  public void initSqlSource(Configuration configuration, String sql) {
    initSqlSource(configuration, sql, null);
  }

  public void initSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return BoundSql.newInstance(configuration, sql, parameterMappings, parameterObject);
  }

  @Override
  public void recyle() {
    handle.recycle(this);
  }
}
