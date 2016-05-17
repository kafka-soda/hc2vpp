/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.translate.impl.write;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCompositeWriter<D extends DataObject> implements Writer<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositeWriter.class);

    private final Map<Class<? extends DataObject>, ChildWriter<? extends ChildOf<D>>> childWriters;
    private final Map<Class<? extends DataObject>, ChildWriter<? extends Augmentation<D>>> augWriters;
    private final InstanceIdentifier<D> instanceIdentifier;
    private TraversalType traversalType;

    public AbstractCompositeWriter(final Class<D> type,
                                   final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                                   final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                                   final TraversalType traversalType) {
        this.traversalType = traversalType;
        this.instanceIdentifier = InstanceIdentifier.create(type);
        this.childWriters = RWUtils.uniqueLinkedIndex(childWriters, RWUtils.MANAGER_CLASS_FUNCTION);
        this.augWriters = RWUtils.uniqueLinkedIndex(augWriters, RWUtils.MANAGER_CLASS_AUG_FUNCTION);
    }

    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("{}: Writing current: {} data: {}", this, id, data);

        switch (traversalType) {
            case PREORDER: {
                LOG.trace("{}: Writing current attributes", this);
                writeCurrentAttributes(id, data, ctx);
                writeChildren(id, data, ctx);
                break;
            }
            case POSTORDER: {
                writeChildren(id, data, ctx);
                LOG.trace("{}: Writing current attributes", this);
                writeCurrentAttributes(id, data, ctx);
                break;
            }
        }

        LOG.debug("{}: Current node written successfully", this);
    }

    private void writeChildren(final InstanceIdentifier<D> id, final D data, final WriteContext ctx)
        throws WriteFailedException {
        for (ChildWriter<? extends ChildOf<D>> child : childWriters.values()) {
            LOG.debug("{}: Writing child in: {}", this, child);
            child.writeChild(id, data, ctx);
        }

        for (ChildWriter<? extends Augmentation<D>> child : augWriters.values()) {
            LOG.debug("{}: Writing augment in: {}", this, child);
            child.writeChild(id, data, ctx);
        }
    }

    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating current: {} dataBefore: {}, datAfter: {}", this, id, dataBefore, dataAfter);

        // FIXME: Equals does not work properly with augments: https://git.opendaylight.org/gerrit/#/c/37719
        // Solution: update mdsal-binding-dom-codec to 0.8.2-Beryllium-SR2 when it will be published in
        // ODL release repository
        //if (dataBefore.equals(dataAfter)) {
        //    LOG.debug("{}: Skipping current(no update): {}", this, id);
        //    // No change, ignore
        //    return;
        //}

        switch (traversalType) {
            case PREORDER: {
                LOG.trace("{}: Updating current attributes", this);
                updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
                updateChildren(id, dataBefore, dataAfter, ctx);
                break;
            }
            case POSTORDER: {
                updateChildren(id, dataBefore, dataAfter, ctx);
                LOG.trace("{}: Updating current attributes", this);
                updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
                break;
            }
        }

        LOG.debug("{}: Current node updated successfully", this);
    }

    private void updateChildren(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                final WriteContext ctx) throws WriteFailedException {
        for (ChildWriter<? extends ChildOf<D>> child : childWriters.values()) {
            LOG.debug("{}: Updating child in: {}", this, child);
            child.updateChild(id, dataBefore, dataAfter, ctx);
        }

        for (ChildWriter<? extends Augmentation<D>> child : augWriters.values()) {
            LOG.debug("{}: Updating augment in: {}", this, child);
            child.updateChild(id, dataBefore, dataAfter, ctx);
        }
    }

    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("{}: Deleting current: {} dataBefore: {}", this, id, dataBefore);

        switch (traversalType) {
            case PREORDER: {
                deleteChildren(id, dataBefore, ctx);
                LOG.trace("{}: Deleting current attributes", this);
                deleteCurrentAttributes(id, dataBefore, ctx);
                break;
            }
            case POSTORDER: {
                LOG.trace("{}: Deleting current attributes", this);
                deleteCurrentAttributes(id, dataBefore, ctx);
                deleteChildren(id, dataBefore, ctx);
                break;
            }
        }
    }

    private void deleteChildren(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx)
        throws WriteFailedException {
        for (ChildWriter<? extends Augmentation<D>> child : reverseCollection(augWriters.values())) {
            LOG.debug("{}: Deleting augment in: {}", this, child);
            child.deleteChild(id, dataBefore, ctx);
        }

        for (ChildWriter<? extends ChildOf<D>> child : reverseCollection(childWriters.values())) {
            LOG.debug("{}: Deleting child in: {}", this, child);
            child.deleteChild(id, dataBefore, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore,
                       @Nullable final DataObject dataAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating : {}", this, id);
        LOG.trace("{}: Updating : {}, from: {} to: {}", this, id, dataBefore, dataAfter);

        if (idPointsToCurrent(id)) {
            if(isWrite(dataBefore, dataAfter)) {
                writeCurrent((InstanceIdentifier<D>) id, castToManaged(dataAfter), ctx);
            } else if(isDelete(dataBefore, dataAfter)) {
                deleteCurrent((InstanceIdentifier<D>) id, castToManaged(dataBefore), ctx);
            } else {
                checkArgument(dataBefore != null && dataAfter != null, "No data to process");
                updateCurrent((InstanceIdentifier<D>) id, castToManaged(dataBefore), castToManaged(dataAfter), ctx);
            }
        } else {
            if (isWrite(dataBefore, dataAfter)) {
                writeSubtree(id, dataAfter, ctx);
            } else if (isDelete(dataBefore, dataAfter)) {
                deleteSubtree(id, dataBefore, ctx);
            } else {
                checkArgument(dataBefore != null && dataAfter != null, "No data to process");
                updateSubtree(id, dataBefore, dataAfter, ctx);
            }
        }
    }

    private void checkDataType(final @Nullable DataObject dataAfter) {
        checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(dataAfter.getClass()));
    }

    private D castToManaged(final DataObject data) {
        checkDataType(data);
        return getManagedDataObjectType().getTargetType().cast(data);
    }

    private static boolean isWrite(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataBefore == null && dataAfter != null;
    }

    private static boolean isDelete(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataAfter == null && dataBefore != null;
    }

    private void writeSubtree(final InstanceIdentifier<? extends DataObject> id,
                              final DataObject dataAfter, final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Writing subtree: {}", this, id);
        final Writer<? extends ChildOf<D>> writer = getNextWriter(id);

        if (writer != null) {
            LOG.debug("{}: Writing subtree: {} in: {}", this, id, writer);
            writer.update(id, null, dataAfter, ctx);
        } else {
            // If there's no dedicated writer, use write current
            // But we need current data after to do so
            final InstanceIdentifier<D> currentId = RWUtils.cutId(id, getManagedDataObjectType());
            Optional<D> currentDataAfter = ctx.readAfter(currentId);
            LOG.debug("{}: Dedicated subtree writer missing for: {}. Writing current.", this,
                RWUtils.getNextId(id, getManagedDataObjectType()).getType(), currentDataAfter);
            writeCurrent(currentId, castToManaged(currentDataAfter.get()), ctx);
        }
    }

    private boolean idPointsToCurrent(final @Nonnull InstanceIdentifier<? extends DataObject> id) {
        return id.getTargetType().equals(getManagedDataObjectType().getTargetType());
    }

    private void deleteSubtree(final InstanceIdentifier<? extends DataObject> id,
                               final DataObject dataBefore, final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Deleting subtree: {}", this, id);
        final Writer<? extends ChildOf<D>> writer = getNextWriter(id);

        if (writer != null) {
            LOG.debug("{}: Deleting subtree: {} in: {}", this, id, writer);
            writer.update(id, dataBefore, null, ctx);
        } else {
            updateSubtreeFromCurrent(id, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateSubtreeFromCurrent(final InstanceIdentifier<? extends DataObject> id, final WriteContext ctx)
        throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.cutId(id, getManagedDataObjectType());
        Optional<D> currentDataBefore = ctx.readBefore(currentId);
        Optional<D> currentDataAfter = ctx.readAfter(currentId);
        LOG.debug("{}: Dedicated subtree writer missing for: {}. Updating current without subtree", this,
            RWUtils.getNextId(id, getManagedDataObjectType()).getType(), currentDataAfter);
        updateCurrent((InstanceIdentifier<D>) id, castToManaged(currentDataBefore.orNull()),
            castToManaged(currentDataAfter.orNull()), ctx);
    }

    private void updateSubtree(final InstanceIdentifier<? extends DataObject> id,
                               final DataObject dataBefore,
                               final DataObject dataAfter,
                               final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating subtree: {}", this, id);
        final Writer<? extends ChildOf<D>> writer = getNextWriter(id);

        if (writer != null) {
            LOG.debug("{}: Updating subtree: {} in: {}", this, id, writer);
            writer.update(id, dataBefore, dataAfter, ctx);
        } else {
            updateSubtreeFromCurrent(id, ctx);
        }
    }

    private Writer<? extends ChildOf<D>> getNextWriter(final InstanceIdentifier<? extends DataObject> id) {
        final Class<? extends DataObject> next = RWUtils.getNextId(id, getManagedDataObjectType()).getType();
        return childWriters.get(next);
    }

    private static <T> List<T> reverseCollection(final Collection<T> original) {
        // TODO find a better reverse mechanism (probably a different collection for child writers is necessary)
        final ArrayList<T> list = Lists.newArrayList(original);
        Collections.reverse(list);
        return list;
    }

    protected abstract void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                   @Nonnull final D data,
                                                   @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final D dataAfter,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }


    @Override
    public String toString() {
        return String.format("Writer[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }
}
