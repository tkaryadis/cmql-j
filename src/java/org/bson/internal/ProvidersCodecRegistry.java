/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.internal;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.bson.assertions.Assertions.isTrueArgument;

public final class ProvidersCodecRegistry implements CodecRegistry, CycleDetectingCodecRegistry {
    private final List<CodecProvider> codecProviders;
    private final CodecCache codecCache = new CodecCache();

    public boolean javaRegistry=true;

    public void setjavaRegistry(boolean isJavaRegistry) {
        this.javaRegistry = isJavaRegistry;
    }

    public boolean getjavaRegistry() {
        return javaRegistry;
    }



    public ProvidersCodecRegistry(final List<? extends CodecProvider> codecProviders) {
        isTrueArgument("codecProviders must not be null or empty", codecProviders != null && codecProviders.size() > 0);
        this.codecProviders = new ArrayList<CodecProvider>(codecProviders);
    }

    @Override
    public <T> Codec<T> get(final Class<T> clazz)
    {
        //System.out.println("i call");
        ChildCodecRegistry c=new ChildCodecRegistry<T>(this, clazz);
        c.setjavaRegistry(javaRegistry);
        return get(c);
    }

    @SuppressWarnings("rawtypes")
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        for (CodecProvider provider : codecProviders) {
            Codec<T> codec = provider.get(clazz, registry);
            if (codec != null) {
                return codec;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes" })
    public <T> Codec<T> get(final ChildCodecRegistry<T> context) {
        if (!codecCache.containsKey(context.getCodecClass())) {
            for (CodecProvider provider : codecProviders) {
                Codec<T> codec = provider.get(context.getCodecClass(), context);
                if (codec != null) {
                    return codecCache.putIfMissing(context.getCodecClass(), codec);
                }
            }
            codecCache.put(context.getCodecClass(), null);
        }
        return codecCache.getOrThrow(context.getCodecClass());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProvidersCodecRegistry that = (ProvidersCodecRegistry) o;
        if (codecProviders.size() != that.codecProviders.size()) {
            return false;
        }
        for (int i = 0; i < codecProviders.size(); i++) {
            if (codecProviders.get(i).getClass() != that.codecProviders.get(i).getClass()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return codecProviders.hashCode();
    }
}
