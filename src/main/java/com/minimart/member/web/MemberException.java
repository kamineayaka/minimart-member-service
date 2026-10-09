package com.minimart.member.web;

public abstract sealed class MemberException extends RuntimeException
		permits MemberException.NotFound, MemberException.Conflict, MemberException.Unauthenticated {

	private final String code;

	private MemberException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static final class NotFound extends MemberException {

		public NotFound(String code, String message) {
			super(code, message, null);
		}
	}

	public static final class Conflict extends MemberException {

		private final String field;

		public Conflict(String code, String message, String field, Throwable cause) {
			super(code, message, cause);
			this.field = field;
		}

		public String field() {
			return field;
		}
	}

	public static final class Unauthenticated extends MemberException {

		public Unauthenticated(String code, String message) {
			super(code, message, null);
		}
	}
}
