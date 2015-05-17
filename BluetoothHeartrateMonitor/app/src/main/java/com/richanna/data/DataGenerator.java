package com.richanna.data;

public interface DataGenerator<T> extends DataProvider<T> {
  public void pause();
  public void resume();
}
