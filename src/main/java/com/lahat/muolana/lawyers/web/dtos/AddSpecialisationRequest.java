package com.lahat.muolana.lawyers.web.dtos;

import jakarta.validation.constraints.NotBlank;

public record AddSpecialisationRequest(@NotBlank String area) {
}

