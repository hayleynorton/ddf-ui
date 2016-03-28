/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.registry.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.registry.api.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.registry.converter.RegistryConversionException;
import ddf.catalog.registry.converter.RegistryPackageConverter;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class RegistryTransformer implements InputTransformer, MetacardTransformer {

    private Parser parser;

    private ParserConfigurator configurator;

    @Override
    public Metacard transform(InputStream inputStream)
            throws IOException, CatalogTransformerException {
        return transform(inputStream, null);
    }

    @Override
    public Metacard transform(InputStream inputStream, String id)
            throws IOException, CatalogTransformerException {

        MetacardImpl metacard;

        try (FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(1000000)) {

            try {
                IOUtils.copy(inputStream, fileBackedOutputStream);

            } catch (IOException e) {
                throw new CatalogTransformerException(
                        "Unable to transform from CSW RIM Service Record to Metacard. Error reading input stream.",
                        e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource()
                    .openStream()) {
                metacard = (MetacardImpl) unmarshal(inputStreamCopy);
            } catch (ParserException e) {
                throw new CatalogTransformerException(
                        "Unable to transform from CSW RIM Service Record to Metacard. Parser exception caught",
                        e);
            } catch (RegistryConversionException e) {
                throw new CatalogTransformerException(
                        "Unable to transform from CSW RIM Service Record to Metacard. Conversion exception caught",
                        e);
            }

            if (metacard == null) {
                throw new CatalogTransformerException(
                        "Unable to transform from CSW RIM Service Record to Metacard.");
            } else if (StringUtils.isNotEmpty(id)) {
                metacard.setAttribute(Metacard.ID, id);
            }

            String xml = CharStreams.toString(fileBackedOutputStream.asByteSource()
                    .asCharSource(Charsets.UTF_8)
                    .openStream());

            metacard.setAttribute(Metacard.METADATA, xml);
            metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));

        } catch (IOException e) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW RIM Service Record to Metacard. Error using file-backed stream.",
                    e);
        }

        return metacard;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        if (metacard.getContentTypeName()
                .startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME)) {
            String metadata = metacard.getMetadata();
            return new BinaryContentImpl(IOUtils.toInputStream(metadata));
        } else {
            throw new CatalogTransformerException(
                    "Can't transform metacard of content type " + metacard.getContentTypeName()
                            + " to csw-ebrim xml");
        }
    }

    private Metacard unmarshal(InputStream xmlStream)
            throws ParserException, RegistryConversionException {
        MetacardImpl metacard = null;

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                configurator,
                JAXBElement.class,
                xmlStream);
        if (registryObjectTypeJAXBElement != null) {

            RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();

            if (registryObjectType != null) {

                metacard =
                        (MetacardImpl) RegistryPackageConverter.getRegistryObjectMetacard(
                                registryObjectType);

            }
        }

        return metacard;
    }

    public void setParser(Parser parser) {

        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());

        this.parser = parser;
    }

}
