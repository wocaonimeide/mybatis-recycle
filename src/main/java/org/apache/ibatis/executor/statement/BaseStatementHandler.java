/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import io.netty.util.Recycler;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
public abstract class BaseStatementHandler implements StatementHandler {

  protected Configuration configuration;
  protected ObjectFactory objectFactory;
  protected TypeHandlerRegistry typeHandlerRegistry;
  protected ResultSetHandler resultSetHandler;
  protected ParameterHandler parameterHandler;

  protected Executor executor;
  protected MappedStatement mappedStatement;
  protected RowBounds rowBounds;

  protected BoundSql boundSql;

  private Boolean proxyed=Boolean.FALSE;

  private Recycler.Handle<BaseStatementHandler> handle;

  protected BaseStatementHandler(Recycler.Handle<? extends BaseStatementHandler> handle){
    Recycler.Handle<BaseStatementHandler> baseStatementHandlerHandle= (Recycler.Handle<BaseStatementHandler>) handle;
    this.handle=baseStatementHandlerHandle;
  }

  protected void initBaseStateHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    if (boundSql == null) { // issue #435, get the key before calculating the statement
      generateKeys(parameterObject);
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    this.boundSql = boundSql;

    this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
  }

  @Override
  public void recyle() {
    this.executor=null;
    this.mappedStatement=null;
    this.rowBounds=null;
    this.boundSql=null;
    this.parameterHandler.recyle();
    this.parameterHandler=null;
    this.resultSetHandler.recyle();
    this.resultSetHandler=null;
    this.handle.recycle(this);
  }

  @Override
  public Boolean checkProxyed() {
    return this.proxyed;
  }

  @Override
  public void changeProxyed(Boolean newProxyedState) {
    proxyed=newProxyedState;
  }

  @Override
  public BoundSql getBoundSql() {
    return boundSql;
  }

  @Override
  public ParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
      statement = instantiateStatement(connection);
      setStatementTimeout(statement, transactionTimeout);
      setFetchSize(statement);
      return statement;
    } catch (SQLException e) {
      closeStatement(statement);
      throw e;
    } catch (Exception e) {
      closeStatement(statement);
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

  protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
    Integer queryTimeout = null;
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (configuration.getDefaultStatementTimeout() != null) {
      queryTimeout = configuration.getDefaultStatementTimeout();
    }
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  }

  protected void setFetchSize(Statement stmt) throws SQLException {
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize != null) {
      stmt.setFetchSize(fetchSize);
      return;
    }
    Integer defaultFetchSize = configuration.getDefaultFetchSize();
    if (defaultFetchSize != null) {
      stmt.setFetchSize(defaultFetchSize);
    }
  }

  protected void closeStatement(Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      //ignore
    }
  }

  protected void generateKeys(Object parameter) {
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    ErrorContext.instance().store();
    keyGenerator.processBefore(executor, mappedStatement, null, parameter);
    ErrorContext.instance().recall();
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return typeHandlerRegistry;
  }

  public ResultSetHandler getResultSetHandler() {
    return resultSetHandler;
  }

  public Executor getExecutor() {
    return executor;
  }

  public MappedStatement getMappedStatement() {
    return mappedStatement;
  }

  public RowBounds getRowBounds() {
    return rowBounds;
  }

  public void setBoundSql(BoundSql boundSql) {
    this.boundSql = boundSql;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  public void setTypeHandlerRegistry(TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  public void setResultSetHandler(ResultSetHandler resultSetHandler) {
    this.resultSetHandler = resultSetHandler;
  }

  public void setParameterHandler(ParameterHandler parameterHandler) {
    this.parameterHandler = parameterHandler;
  }

  public void setExecutor(Executor executor) {
    this.executor = executor;
  }

  public void setMappedStatement(MappedStatement mappedStatement) {
    this.mappedStatement = mappedStatement;
  }

  public void setRowBounds(RowBounds rowBounds) {
    this.rowBounds = rowBounds;
  }
}
