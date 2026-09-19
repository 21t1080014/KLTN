package com.dungochung.shopdongho.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.dungochung.shopdongho.common.CurrentActor;
import com.dungochung.shopdongho.common.PaginationCommon;
import com.dungochung.shopdongho.common.constant.Constant;
import com.dungochung.shopdongho.dto.ResponseDataDto;
import com.dungochung.shopdongho.entity.AuditLogEntity;
import com.dungochung.shopdongho.repository.AuditLogRepository;
import com.dungochung.shopdongho.service.AuditService;

@Service
public class AuditServiceImpl implements AuditService {
	@Autowired
	private AuditLogRepository repository;

	@Override
	public void log(String action, String targetType, String targetId, String detail) {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		String role = attrs == null ? null : (String) attrs.getAttribute("roleName", RequestAttributes.SCOPE_SESSION);
		logAs(CurrentActor.username(), role, action, targetType, targetId, detail);
	}

	@Override
	public void logAs(String actor, String role, String action, String targetType, String targetId, String detail) {
		AuditLogEntity a = new AuditLogEntity();
		a.setActor(cut(actor, 100));
		a.setActorRole(cut(role, 30));
		a.setAction(cut(action, 40));
		a.setTargetType(cut(targetType, 40));
		a.setTargetId(cut(targetId, 100));
		a.setDetail(cut(detail, 500));
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		if (attrs instanceof ServletRequestAttributes sra) {
			a.setIp(cut(sra.getRequest().getRemoteAddr(), 64));
		}
		repository.save(a);
	}

	@Override
	public ResponseDataDto search(String action, String actor, int page, int size) {
		String act = action == null || action.isBlank() ? null : action.trim();
		String who = actor == null || actor.isBlank() ? null
				: "%" + actor.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
		int safePage = Math.max(page, 0);
		Page<AuditLogEntity> result = repository.search(act, who,
				PageRequest.of(safePage, Math.min(Math.max(size, 1), 100)));
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		List<Map<String, Object>> rows = new ArrayList<>();
		for (AuditLogEntity a : result.getContent()) {
			Map<String, Object> m = new HashMap<>();
			m.put("at", a.getCreatedAt().format(fmt));
			m.put("actor", a.getActor());
			m.put("role", a.getActorRole());
			m.put("action", a.getAction());
			m.put("targetType", a.getTargetType());
			m.put("targetId", a.getTargetId());
			m.put("detail", a.getDetail());
			m.put("ip", a.getIp());
			rows.add(m);
		}
		Map<String, Object> data = new HashMap<>();
		data.put("logs", rows);
		data.put("totalCount", result.getTotalElements());
		data.put("pagination", PaginationCommon.getPaginationInfo(safePage, result.getTotalPages(), 3));
		return new ResponseDataDto(Constant.RESULT_CD_SUCCESS, "OK", data);
	}

	private static String cut(String s, int max) {
		return s == null || s.length() <= max ? s : s.substring(0, max);
	}
}
