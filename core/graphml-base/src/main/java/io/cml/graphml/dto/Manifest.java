/*
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

package io.cml.graphml.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Manifest
{
    private final List<Model> models;
    private final List<Relationship> relationships;

    private final List<EnumDefinition> enumDefinitions;

    public static Manifest manifest(List<Model> models, List<Relationship> relationships, List<EnumDefinition> enumDefinitions)
    {
        return new Manifest(models, relationships, enumDefinitions);
    }

    @JsonCreator
    public Manifest(
            @JsonProperty("models") List<Model> models,
            @JsonProperty("relationships") List<Relationship> relationships,
            @JsonProperty("enums") List<EnumDefinition> enumDefinitions)
    {
        this.models = models == null ? List.of() : models;
        this.relationships = relationships == null ? List.of() : relationships;
        this.enumDefinitions = enumDefinitions == null ? List.of() : enumDefinitions;
    }

    public List<Model> getModels()
    {
        return models;
    }

    public List<Relationship> getRelationships()
    {
        return relationships;
    }

    public List<EnumDefinition> getEnumFields()
    {
        return enumDefinitions;
    }
}