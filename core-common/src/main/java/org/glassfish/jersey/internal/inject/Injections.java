/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

/**
 * HK2 injection binding utility methods.
 *
 * @author Tom Beerbower
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Injections {
    private final static ServiceLocatorGenerator generator = new ServiceLocatorGeneratorImpl();
    private final static ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();

    public static DynamicConfiguration getConfiguration(ServiceLocator locator) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        return dcs.createDynamicConfiguration();
    }

    /**
     * Create a {@link ServiceLocator}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param binders the HK2 binders.
     * @return a service locator with all the bindings.
     */
    public static ServiceLocator createLocator(String name, ServiceLocator parent, Binder... binders) {
        ServiceLocator locator = factory.create(name, parent, generator);

        ServiceLocatorUtilities.enablePerThreadScope(locator);

        for (Binder binder : binders) {
            bind(locator, binder);
        }

        return locator;
    }

    /**
     * Create a {@link ServiceLocator}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param name    the name of the service locator to create; may be {@code null}.
     * @param binders the HK2 binders.
     * @return a service locator with all the bindings.
     */
    public static ServiceLocator createLocator(String name, Binder... binders) {

        ServiceLocator locator = factory.create(name, null, generator);

        ServiceLocatorUtilities.enablePerThreadScope(locator);

        for (Binder binder : binders) {
            bind(locator, binder);
        }

        return locator;
    }

    /**
     * Create an unnamed {@link ServiceLocator}.
     *
     * @param binders the HK2 binders.
     * @return a service locator with all the bindings.
     */
    public static ServiceLocator createLocator(Binder... binders) {
        ServiceLocator locator = factory.create(null, null, generator);

        ServiceLocatorUtilities.enablePerThreadScope(locator);

        for (Binder binder : binders) {
            bind(locator, binder);
        }

        return locator;
    }

    private static void bind(ServiceLocator locator, Binder binder) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration dc = dcs.createDynamicConfiguration();

        locator.inject(binder);
        binder.bind(dc);

        dc.commit();
    }

    /**
     * Get the class by contract or create and inject a new instance.
     *
     * @param <T>            instance type.
     * @param serviceLocator HK2 service locator.
     * @param clazz          class of the instance to be provider.
     * @return instance of the class either provided as a service or created and injected  by HK2.
     */
    public static <T> T getOrCreate(ServiceLocator serviceLocator, Class<T> clazz) {
        T component = serviceLocator.getService(clazz);
        return component == null ? serviceLocator.createAndInitialize(clazz) : component;
    }

    /**
     * Add a binding represented by the binding builder to the HK2 dynamic configuration.
     *
     * @param builder       binding builder.
     * @param configuration HK2 dynamic configuration.
     */
    public static void addBinding(BindingBuilder<?> builder, DynamicConfiguration configuration) {
        if (builder instanceof AbstractBindingBuilder) {
            ((AbstractBindingBuilder<?>) builder).complete(configuration, null);
        } else {
            throw new IllegalArgumentException("Unknown binding builder type: " + builder.getClass().getName());
        }
    }

    /**
     * Add a binding represented by the binding builder to the HK2 dynamic configuration.
     *
     * @param builder       binding builder.
     * @param configuration HK2 dynamic configuration.
     * @param defaultLoader default HK2 service loader that should be used to load the service class
     *                      in case a custom loader has not been set.
     */
    public static void addBinding(BindingBuilder<?> builder, DynamicConfiguration configuration, HK2Loader defaultLoader) {
        if (builder instanceof AbstractBindingBuilder) {
            ((AbstractBindingBuilder<?>) builder).complete(configuration, defaultLoader);
        } else {
            throw new IllegalArgumentException("Unknown binding builder type: " + builder.getClass().getName());
        }
    }

    /**
     * Get a new factory class-based service binding builder.
     *
     * @param <T>          service type.
     * @param factoryType  service factory class.
     * @param factoryScope factory scope.
     * @return initialized binding builder.
     */
    public static <T> ServiceBindingBuilder<T> newFactoryBinder(
            Class<? extends Factory<T>> factoryType, Class<? extends Annotation> factoryScope) {
        return AbstractBindingBuilder.<T>createFactoryBinder(factoryType, factoryScope);
    }

    /**
     * Get a new factory class-based service binding builder.
     *
     * The factory itself is bound in a {@link org.glassfish.hk2.api.PerLookup per-lookup} scope.
     *
     * @param <T>         service type.
     * @param factoryType service factory class.
     * @return initialized binding builder.
     */
    public static <T> ServiceBindingBuilder<T> newFactoryBinder(Class<? extends Factory<T>> factoryType) {
        return AbstractBindingBuilder.<T>createFactoryBinder(factoryType, null);
    }

    /**
     * Get a new factory instance-based service binding builder.
     *
     * @param <T>     service type.
     * @param factory service instance.
     * @return initialized binding builder.
     */
    public static <T> ServiceBindingBuilder<T> newFactoryBinder(Factory<T> factory) {
        return AbstractBindingBuilder.createFactoryBinder(factory);
    }

    /**
     * Get a new class-based service binding builder.
     *
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public static <T> ServiceBindingBuilder<T> newBinder(Class<T> serviceType) {
        return AbstractBindingBuilder.create(serviceType, false);
    }

    /**
     * Get a new instance-based service binding builder. The binding is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     *
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>     service type.
     * @param service service instance.
     * @return initialized binding builder.
     */
    public static <T> ScopedBindingBuilder<T> newBinder(T service) {
        return AbstractBindingBuilder.create(service);
    }
}