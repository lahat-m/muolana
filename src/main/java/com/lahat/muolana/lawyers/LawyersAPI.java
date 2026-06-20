package com.lahat.muolana.lawyers;

import com.lahat.muolana.lawyers.domain.LawyerService;
import org.springframework.stereotype.Component;

@Component
public class LawyersAPI {

    private final LawyerService lawyerService;

    public LawyersAPI(LawyerService lawyerService) {
        this.lawyerService = lawyerService;
    }

}
