package org.hisp.dhis.dxf2.events.event.validation;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.context.WorkContext;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Luciano Fiandesio
 */
public class ProgramStageCheckTest
{
    private ProgramStageCheck rule;

    private Event event;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkContext workContext;

    @Mock
    private ServiceDelegator serviceDelegator;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp()
    {
        rule = new ProgramStageCheck();
        event = createBaseEvent();
    }

    @Test
    public void failOnNullProgramStage()
    {
        ImportSummary summary = rule.check( new ImmutableEvent( event ), null );
        assertHasError( summary, event,
            "Event.programStage does not point to a valid programStage: " + event.getProgramStage() );
    }

    @Test
    public void failOnNonRepeatableStageAndExistingEvents()
    {
        // Data preparation
        event.setProgramStage( CodeGenerator.generateUid() );

        Program program = createProgram( 'P' );
        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.setRepeatable( false );

        when( workContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();
        ProgramInstance programInstance = new ProgramInstance();
        programInstanceMap.put( event.getUid(), programInstance );

        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        when( workContext.getServiceDelegator() ).thenReturn( serviceDelegator );
        when( serviceDelegator.getJdbcTemplate() ).thenReturn( jdbcTemplate );
        when( jdbcTemplate.queryForObject( anyString(), eq( Boolean.class ), eq( programStage.getUid() ) ) )
            .thenReturn( true );

        // Method under test
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
                "Program stage is not repeatable and an event already exists" );
    }

    private void assertHasError( ImportSummary summary, Event event, String description )
    {
        assertThat( summary, is( notNullValue() ) );
        assertThat( summary.getImportCount().getIgnored(), is( 1 ) );
        assertThat( summary.getReference(), is( event.getUid() ) );
        assertThat( summary.getStatus(), is( ImportStatus.ERROR ) );
        assertThat( summary.getDescription(), is( description ) );
    }

    private Event createBaseEvent()
    {
        Event event = new Event();
        String uid = CodeGenerator.generateUid();
        event.setUid( uid );
        event.setEvent( uid );
        return event;
    }

}