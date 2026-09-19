package com.dungochung.shopdongho.common;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.dungochung.shopdongho.entity.UserEntity;

/** Lấy tên người đang thao tác từ session (admin: "userLogin", khách: "currentUser"); ngoài request thì "system". */
public final class CurrentActor {
	private CurrentActor() {
	}

	public static String username() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return "system";
		}
		for (String key : new String[] { "userLogin", "currentUser" }) {
			Object user = attrs.getAttribute(key, RequestAttributes.SCOPE_SESSION);
			if (user instanceof UserEntity u && u.getUsername() != null) {
				return u.getUsername();
			}
		}
		return "system";
	}
}
