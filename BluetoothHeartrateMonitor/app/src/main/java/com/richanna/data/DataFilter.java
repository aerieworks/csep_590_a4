package com.richanna.data;

import com.richanna.events.Listener;

public interface DataFilter<T, U> extends Listener<T>, DataProvider<U> {
}
