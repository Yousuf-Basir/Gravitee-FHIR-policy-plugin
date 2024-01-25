/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.policy.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;

@Slf4j
public class JsonToFhirConverter {

    public static String convert(String patientJson) {
        System.out.println("Input JSON: " + patientJson);
        // System.out.println("data: " + data);

        ObjectMapper objectMapper = new ObjectMapper();
        FhirContext ctx = FhirContext.forDstu3();
        try {
            JsonNode rootNode = objectMapper.readTree(patientJson);
            Patient patient = jsonToPatient(rootNode);
            // Output the FHIR patient in JSON format
            String encoded = ctx.newJsonParser().encodeResourceToString(patient);
            System.out.println(encoded);

            return encoded;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Patient jsonToPatient(JsonNode jsonNode) {
        Patient patient = new Patient();

        // Assuming id is a string
        if (jsonNode.has("id")) {
            patient.setId(jsonNode.get("id").asText());
        }

        if (jsonNode.has("name")) {
            patient.addName().setFamily(jsonNode.get("name").asText());
        }

        if (jsonNode.has("gender")) {
            String gender = jsonNode.get("gender").asText();
            patient.setGender(
                "male".equals(gender)
                    ? AdministrativeGender.MALE
                    : "female".equals(gender) ? AdministrativeGender.FEMALE : AdministrativeGender.UNKNOWN
            );
        }

        if (jsonNode.has("address")) {
            Address address = patient.addAddress();
            address.addLine(jsonNode.get("address").asText());
        }

        // Add other fields similarly

        return patient;
    }
}
