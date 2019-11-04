/**
 *    Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.scripting.xmltags;

import io.netty.util.Recycler;
import ognl.OgnlContext;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author Clinton Begin
 */
public class DynamicContext {

  public static final String PARAMETER_OBJECT_KEY = "_parameter";
  public static final String DATABASE_ID_KEY = "_databaseId";

  private static final Recycler<DynamicContext> RECYCLER=new Recycler<DynamicContext>() {
    @Override
    protected DynamicContext newObject(Handle<DynamicContext> handle) {
      return new DynamicContext(handle);
    }
  };

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  private ContextMap bindings;
  private StringJoiner sqlBuilder = new StringJoiner(" ");
  private int uniqueNumber = 0;
  private Recycler.Handle<DynamicContext> handle;

  private DynamicContext(Recycler.Handle<DynamicContext> handle){
    this.handle=handle;
  }

  public static DynamicContext newInstance(Configuration configuration, Object parameterObject) {
    DynamicContext context=RECYCLER.get();
    context.initContext(configuration, parameterObject);
    return context;
  }

  private void initContext(Configuration configuration, Object parameterObject) {
    if (parameterObject != null && !(parameterObject instanceof Map)) {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
      bindings = ContextMap.newInstance(metaObject, existsTypeHandler);
    } else {
      bindings = ContextMap.newInstance(null, false);
    }
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
  }

  public Map<String, Object> getBindings() {
    return bindings;
  }

  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  public void appendSql(String sql) {
    sqlBuilder.add(sql);
  }

  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  public int getUniqueNumber() {
    return uniqueNumber++;
  }

  static class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2977601501966151582L;
    private static final Recycler<ContextMap> RECYCLER=new Recycler<ContextMap>() {
      @Override
      protected ContextMap newObject(Handle<ContextMap> handle) {
        return new ContextMap(handle);
      }
    };
    private MetaObject parameterMetaObject;
    private boolean fallbackParameterObject;

    private Recycler.Handle<ContextMap> handle;

    private ContextMap(Recycler.Handle<ContextMap> handle){
      this.handle=handle;
    }

    public static ContextMap newInstance(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
      ContextMap map=RECYCLER.get();
      map.initMap(parameterMetaObject,fallbackParameterObject);
      return map;
    }

    public void initMap(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
      this.parameterMetaObject = parameterMetaObject;
      this.fallbackParameterObject = fallbackParameterObject;
    }

    @Override
    public Object get(Object key) {
      String strKey = (String) key;
      if (super.containsKey(strKey)) {
        return super.get(strKey);
      }

      if (parameterMetaObject == null) {
        return null;
      }

      if (fallbackParameterObject && !parameterMetaObject.hasGetter(strKey)) {
        return parameterMetaObject.getOriginalObject();
      } else {
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
      }
    }

    public void recycler(){
      handle.recycle(this);
    }
  }

  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(Map context, Object target, Object name) {
      Map map = (Map) target;

      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map)parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(Map context, Object target, Object name, Object value) {
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }

  public void recycler(){
    this.bindings.recycler();
    handle.recycle(this);
  }
}