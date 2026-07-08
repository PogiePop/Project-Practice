package com.audit.common;

import java.util.List;

public class PageResult<T> {
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;
    private List<T> list;

    public PageResult(long total, int pageNum, int pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
        this.list = list;
    }

    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public int getPages() { return pages; }
    public List<T> getList() { return list; }
}
