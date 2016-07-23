package com.android.dialer.calllog;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.ArrayList;

/**
 * 用来重新排序Cursor
 */



public class CallLogSortCursor extends CursorWrapper{

    static class SortEntry {
        //这三个属性用来判断是否需要group 即排序的依据
        String currentNumber;
        String currentAccountComponentName;
        String currentAccountId;
        int callType;//是否语音通话
        int order;  //相当于原始cursor的指针。
    }

    public CallLogSortCursor(Cursor cursor) {
        super(cursor);
    }

    Cursor mCursor;
    ArrayList<SortEntry> sortList = null;
    int mPos = -1;



    public CallLogSortCursor(Cursor cursor, ArrayList<SortEntry> list) {
        super(cursor);
        mCursor = cursor;
        sortList = list;
    }

    public boolean moveToPosition(int position) {

        if (position < 0) {
            position = 0;
        }
        if (position >= sortList.size()) {
            position = sortList.size()-1;
        }
        mPos = position;
        int order = sortList.get(position).order;
        return mCursor.moveToPosition(order);
    }
    public boolean moveToFirst() {
        return moveToPosition(0);
    }
    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }
    public boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }
    public boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }
    public boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }
    public int getPosition() {
        return mPos;
    }
}
