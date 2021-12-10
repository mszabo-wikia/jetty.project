//
// ========================================================================
// Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

module org.eclipse.jetty.annotations6
{
    requires jakarta.annotation;
    requires java.naming;
    requires org.slf4j;

    requires transitive org.eclipse.jetty.plus6;
    requires transitive org.objectweb.asm;
    requires jetty.servlet.api;
    requires org.eclipse.jetty.webapp;

    exports org.eclipse.jetty.annotations;

    uses jakarta.servlet.ServletContainerInitializer;

    provides org.eclipse.jetty.webapp.Configuration with
        org.eclipse.jetty.annotations.AnnotationConfiguration;
}