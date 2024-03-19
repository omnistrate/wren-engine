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

package io.accio.base.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.accio.base.Utils.requireNonNullEmpty;
import static java.util.Objects.requireNonNull;

public class EnumDefinition
{
    public static EnumDefinition enumDefinition(String name, List<EnumValue> values)
    {
        return new EnumDefinition(name, values, ImmutableMap.of());
    }

    private final String name;
    private final List<EnumValue> values;
    private final Map<String, String> properties;

    @JsonCreator
    public EnumDefinition(
            @JsonProperty("name") String name,
            @JsonProperty("values") List<EnumValue> values,
            @JsonProperty("properties") Map<String, String> properties)
    {
        this.name = requireNonNullEmpty(name, "name is null or empty");
        this.values = requireNonNull(values);
        this.properties = properties == null ? ImmutableMap.of() : properties;
    }

    @JsonProperty
    public List<EnumValue> getValues()
    {
        return values;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    @JsonProperty
    public Map<String, String> getProperties()
    {
        return properties;
    }

    public Optional<EnumValue> valueOf(String enumValueName)
    {
        return values.stream()
                .filter(candidate -> candidate.getName().equals(enumValueName))
                .findAny();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EnumDefinition that = (EnumDefinition) obj;
        return Objects.equals(name, that.name) &&
                Objects.equals(values, that.values) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
                name,
                values,
                properties);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("name", name)
                .add("values", values)
                .add("properties", properties)
                .toString();
    }
}
