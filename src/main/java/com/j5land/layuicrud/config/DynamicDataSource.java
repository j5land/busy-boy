package com.j5land.layuicrud.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

  //private static final ThreadLocal<DataSource> dataSource = ThreadLocal.withInitial(() -> (DataSource) SpringUtils.getBean("defaultDataSource"));

  private static DataSource nowDataSource;

  public static void setDataSource(DataSource dataSource) {
    //DynamicDataSource.dataSource.set(dataSource);
    nowDataSource = dataSource;
  }

  public static DataSource getDataSource() {
    //return DynamicDataSource.dataSource.get();
    return nowDataSource;
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return null;
  }

  @Override
  protected DataSource determineTargetDataSource() {
    return getDataSource();
  }

  public static void clear(){
    DynamicDataSource.nowDataSource = null;
  }

}