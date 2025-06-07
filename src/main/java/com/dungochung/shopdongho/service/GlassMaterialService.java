package com.dungochung.shopdongho.service;

import com.dungochung.shopdongho.dto.ResponseDataDto;

public interface GlassMaterialService {
	ResponseDataDto getAllWatchGlass(int page,int size);
	ResponseDataDto creatWGlass(String name);
	ResponseDataDto updateWGlass(Integer glassId, String name);
	ResponseDataDto deleteWGlass(Integer glassId);
	ResponseDataDto searchWGlass(String keywword,int page,int size);
}
