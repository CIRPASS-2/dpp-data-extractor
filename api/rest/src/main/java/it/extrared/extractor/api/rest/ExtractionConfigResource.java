/*
 * Copyright 2024-2027 CIRPASS-2
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.extrared.extractor.api.rest;

import io.smallrye.mutiny.Uni;
import it.extrared.extractor.config.ExtractionConfiguration;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Resource interface for REST endpoint to perform read/write operation over an {@link
 * ExtractionConfiguration}.
 */
@Path("/config/v1")
public interface ExtractionConfigResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Add an Extraction configuration",
            description =
                    "Add an Extraction configuration. If a configuration already exists the newly added one becomes the current configuration in use.")
    Uni<RestResponse<Void>> addConfig(ExtractionConfiguration configuration);

    @GET
    @Path("/current")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Retrieve the current Extraction configuration",
            description =
                    "Retrieve the Extraction configuration currently in use by the API,i.e. the last added.")
    Uni<RestResponse<ExtractionConfiguration>> getCurrent();

    @DELETE
    @Path("/current")
    @Operation(
            summary = "Remove the current Extraction configuration",
            description =
                    "Remove the Extraction configuration currently in use by the API,i.e. the last added, causing the previous JSON schema, if any, to become the new current schema.")
    Uni<RestResponse<Void>> removeCurrent();
}
