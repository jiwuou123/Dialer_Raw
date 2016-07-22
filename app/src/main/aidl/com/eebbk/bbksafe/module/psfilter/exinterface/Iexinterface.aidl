package com.eebbk.bbksafe.module.psfilter.exinterface;
import android.content.Intent;
import android.os.Bundle;
interface Iexinterface
{
	boolean isFilterMumber(in Bundle bundle);
	boolean addPhoneInterceptRecord(in Bundle bundle);
	boolean addSmsInterceptRecord(in Bundle bundle);
	
	boolean addBlacklist(String number);
	boolean isBlacklist(String number);
	boolean removeBlacklist(String number);
	
	boolean addWhitelist(String number);
	boolean isWhitelist(String number);
	boolean removeWhitelist(String number);
}