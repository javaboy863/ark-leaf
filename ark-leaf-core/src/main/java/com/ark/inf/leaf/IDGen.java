package com.ark.inf.leaf;

import com.ark.inf.leaf.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
