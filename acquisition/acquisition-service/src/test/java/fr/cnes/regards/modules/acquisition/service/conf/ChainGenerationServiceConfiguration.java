/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.service.conf;

import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Configuration
@PropertySource(value = { "classpath:application-test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
@ComponentScan(basePackages = {"fr.cnes.regards.modules.acquisition"})
@EnableAutoConfiguration
public class ChainGenerationServiceConfiguration {
    
    @Bean
    public IAuthenticationResolver authenticationResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

}