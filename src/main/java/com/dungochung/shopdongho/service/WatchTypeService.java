package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface WatchTypeService {
	ResponseDataDto getAllWatchType(int page,int size);
	ResponseDataDto creatWType(String name);
	ResponseDataDto updateWType(Integer typeId, String name);
	ResponseDataDto deleteWType(Integer typeId);
	ResponseDataDto searchWType(String keywword,int page,int size);
}
