package com.uniwise.course_service.modules.question_banks.impl;

import com.uniwise.course_service.modules.question_banks.QuestionBankService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuestionBankServiceImpl implements QuestionBankService{
    @Override
    public Object createBank() {
        String raw = "abc";
        log.info(raw);
        return null;
    }
}
