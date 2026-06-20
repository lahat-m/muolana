package com.lahat.muolana.auth.domain;

public record RegisterCmd(String email, String password, String fullName) {
}
