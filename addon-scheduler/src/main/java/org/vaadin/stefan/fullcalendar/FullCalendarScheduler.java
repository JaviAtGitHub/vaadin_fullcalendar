/*
 * Copyright 2018, Stefan Uebe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.vaadin.stefan.fullcalendar;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;
import elemental.json.Json;
import elemental.json.JsonArray;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Flow implementation for the FullCalendar.
 * <p>
 * Please visit <a href="https://fullcalendar.io/">https://fullcalendar.io/</a> for details about the client side
 * component, API, functionality, etc.
 */
@NpmPackage(value = "@fullcalendar/resource-timeline", version = "^4.3.0")
@NpmPackage(value = "@fullcalendar/resource-timegrid", version = "^4.3.0")
@Tag("full-calendar-scheduler")
@JsModule("./full-calendar-scheduler.js")
public class FullCalendarScheduler extends FullCalendar implements Scheduler {

    private final Map<String, Resource> resources = new HashMap<>();

    /**
     * Creates a default instance.
     */
    public FullCalendarScheduler() {
        super();
    }

    /**
     * Creates a new instance using the given integer as entries shown per day limit
     * @param entryLimit max entries shown per day
     */
    public FullCalendarScheduler(int entryLimit) {
        super(entryLimit);
    }

    @Override
    public void setSchedulerLicenseKey(String schedulerLicenseKey) {
        setOption("schedulerLicenseKey", schedulerLicenseKey);
    }


    @Override
    public void addResources(@NotNull Iterable<Resource> iterableResource) {
        Objects.requireNonNull(iterableResource);

        JsonArray array = Json.createArray();
        iterableResource.forEach(resource -> {
            String id = resource.getId();
            if (!resources.containsKey(id)) {
                resources.put(id, resource);
                array.set(array.length(), resource.toJson()); // this automatically sends sub resources to the client side
            }

            // now also register child resources
            registerResourcesInternally(resource.getChildren());
        });

        getElement().callJsFunction("addResources", array);
    }

    /**
     * Adds resources to the internal resources map. Does not update the client side. This method is mainly intended
     * to be used for child resources of registered resources, as the toJson method takes care for recursive child registration
     * on the client side, thus no separate call of toJson for children is needed.
     * @param resources resources
     */
    private void registerResourcesInternally(Collection<Resource> resources) {
        for (Resource resource : resources) {
            this.resources.put(resource.getId(), resource);
            registerResourcesInternally(resource.getChildren());
        }
    }

    @Override
    public void removeResources(@NotNull Iterable<Resource> iterableResources) {
        Objects.requireNonNull(iterableResources);

        removeFromEntries(iterableResources);

        // create registry of removed items to send to client
        JsonArray array = Json.createArray();
        iterableResources.forEach(resource -> {
            String id = resource.getId();
            if (this.resources.containsKey(id)) {
                this.resources.remove(id);
                array.set(array.length(), resource.toJson());
            }
        });

        getElement().callJsFunction("removeResources", array);

    }

    /**
     * Removes the given resources from the known entries of this calendar.
     * @param iterableResources resources
     */
    private void removeFromEntries(Iterable<Resource> iterableResources) {
        List<Resource> resources = StreamSupport.stream(iterableResources.spliterator(), false).collect(Collectors.toList());
        getEntries().stream().filter(e -> e instanceof ResourceEntry).forEach(e -> ((ResourceEntry) e).unassignResources(resources));
    }

    @Override
    public Optional<Resource> getResourceById(@NotNull String id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(resources.get(id));
    }

    @Override
    public Set<Resource> getResources() {
        return new LinkedHashSet<>(resources.values());
    }

    @Override
    public void removeAllResources() {
        removeFromEntries(resources.values());
    	resources.clear();
        getElement().callJsFunction("removeAllResources");
    }
    

    @Override
    public void setResourceRenderCallback(String s) {
        getElement().callJsFunction("setResourceRenderCallback", s);
    }


    @Override
    public void setGroupEntriesBy(GroupEntriesBy groupEntriesBy) {
        switch (groupEntriesBy) {
            default:
            case NONE:
                setOption("groupByResource", false);
                setOption("groupByDateAndResource", false);
                break;
            case RESOURCE_DATE:
                setOption("groupByDateAndResource", false);
                setOption("groupByResource", true);
                break;
            case DATE_RESOURCE:
                setOption("groupByResource", false);
                setOption("groupByDateAndResource", true);
                break;
        }
    }


    @Override
    public Registration addTimeslotsSelectedListener(@NotNull ComponentEventListener<? extends TimeslotsSelectedEvent> listener) {
        return addTimeslotsSelectedSchedulerListener((ComponentEventListener) listener);
    }

    @Override
    public Registration addTimeslotsSelectedSchedulerListener(@NotNull ComponentEventListener<? extends TimeslotsSelectedSchedulerEvent> listener) {
        Objects.requireNonNull(listener);
        return addListener(TimeslotsSelectedSchedulerEvent.class, (ComponentEventListener) listener);
    }

    @Override
    public Registration addTimeslotClickedListener(@NotNull ComponentEventListener<? extends TimeslotClickedEvent> listener) {
        return addTimeslotClickedSchedulerListener((ComponentEventListener) listener);
    }

    @Override
    public Registration addTimeslotClickedSchedulerListener(@NotNull ComponentEventListener<? extends TimeslotClickedSchedulerEvent> listener) {
        Objects.requireNonNull(listener);
        return addListener(TimeslotClickedSchedulerEvent.class, (ComponentEventListener) listener);
    }

    /**
     * Registers a listener to be informed when an entry dropped event occurred, along with scheduler
     * specific data.
     *
     * @deprecated misspelled method name, will be removed in future. Please
     * use {@link #addEntryDroppedSchedulerListener(ComponentEventListener)} instead
     *
     * @param listener listener
     * @return registration to remove the listener
     * @throws NullPointerException when null is passed
     */
    @Deprecated
    public Registration addEntryDroppedScedulerListener(@NotNull ComponentEventListener<? extends EntryDroppedSchedulerEvent> listener) {
        return addEntryDroppedSchedulerListener(listener);
    }

    @Override
    public Registration addEntryDroppedSchedulerListener(@NotNull ComponentEventListener<? extends EntryDroppedSchedulerEvent> listener) {
        Objects.requireNonNull(listener);
        return addListener(EntryDroppedSchedulerEvent.class, (ComponentEventListener) listener);
    }
}
