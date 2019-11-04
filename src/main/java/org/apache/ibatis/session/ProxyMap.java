package org.apache.ibatis.session;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMap<k,v> extends ConcurrentHashMap<k,v> {

    private Integer proxyMapSize;

    private Configuration configuration;

    public ProxyMap(Configuration configuration){
        this.configuration=configuration;
        Properties properties=configuration.variables;
        Integer mapSize=null;
        try {
            String sizeTmep=properties.getProperty("proxyMapSize");
            if (null!=sizeTmep && !"".equals(sizeTmep)){
                mapSize=Integer.valueOf(sizeTmep);
            }else {
                mapSize=512;
            }
        }catch (Exception e){
            // do noting
            mapSize=512;
        }
        proxyMapSize=mapSize;
    }

    @Override
    public v put( k key,  v value) {
        Integer contains=this.size();
        if (contains>=proxyMapSize){
            return value;
        }else {
            return super.put(key, value);
        }
    }
}
