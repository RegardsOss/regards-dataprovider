/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.rest;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * {@link AcquisitionProcessingChain} REST module controller
 * @author Christophe Mertz
 */
@RestController
@RequestMapping(AcquisitionProcessingChainController.TYPE_PATH)
public class AcquisitionProcessingChainController implements IResourceController<AcquisitionProcessingChain> {

    public static final String TYPE_PATH = "/chains";

    public static final String CHAIN_PATH_PARAM = "chainId";

    public static final String CHAIN_PATH = "/{" + CHAIN_PATH_PARAM + "}";

    public static final String START_MANUAL_CHAIN_PATH = CHAIN_PATH + "/start";

    public static final String RELAUNCH_ERRORS_PATH = "/relaunch";

    public static final String STOP_CHAIN_PATH = CHAIN_PATH + "/stop";

    public static final String CHAIN_SESSION_PRODUCTS_PATH = "/products";

    @Autowired
    private IAcquisitionProcessingService processingService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link AcquisitionProcessingChain}
     * @param pageable a {@link Pageable} for pagination information
     * @param assembler a {@link ResourceAssembler} to easily convert {@link Page} instances into {@link PagedModel}
     * @return {@link List} of {@link EntityModel} of {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "List all the chains", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<AcquisitionProcessingChain>>> retrieveAll(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AcquisitionProcessingChain> assembler) throws ModuleException {
        return new ResponseEntity<>(toPagedResources(processingService.getFullChains(pageable), assembler),
                HttpStatus.OK);
    }

    /**
     * Create a {@link AcquisitionProcessingChain}
     * @param processingChain the {@link AcquisitionProcessingChain} to create
     * @return the created {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Add a chain", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> create(
            @Valid @RequestBody AcquisitionProcessingChain processingChain) throws ModuleException {
        return new ResponseEntity<>(toResource(processingService.createChain(processingChain)), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PATCH)
    @ResourceAccess(description = "Patch several acquisition chains with new state and mode",
            role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<AcquisitionProcessingChain>>> updateChainsStateAndMode(
            @Valid @RequestBody UpdateAcquisitionProcessingChains payload) throws ModuleException {
        return new ResponseEntity<>(toResources(processingService.patchChainsStateAndMode(payload)), HttpStatus.OK);
    }

    /**
     * Get a {@link AcquisitionProcessingChain}
     * @param chainId the {@link AcquisitionProcessingChain} identifier
     * @return the retrieved {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.GET, value = CHAIN_PATH)
    @ResourceAccess(description = "Get a chain", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> get(@PathVariable Long chainId)
            throws ModuleException {
        return ResponseEntity.ok(toResource(processingService.getChain(chainId)));
    }

    /**
     * Update a {@link AcquisitionProcessingChain}
     * @param chainId the {@link AcquisitionProcessingChain} identifier to update
     * @param processingChain the {@link AcquisitionProcessingChain} to update
     * @return the updated {@link AcquisitionProcessingChain}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.PUT, value = CHAIN_PATH)
    @ResourceAccess(description = "Update a chain", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> update(@PathVariable Long chainId,
            @Valid @RequestBody AcquisitionProcessingChain processingChain) throws ModuleException {
        return ResponseEntity.ok(toResource(processingService.updateChain(processingChain)));
    }

    @RequestMapping(method = RequestMethod.PATCH, value = CHAIN_PATH)
    @ResourceAccess(description = "Patch the state and the mode of the chain", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> updateStateAndMode(@PathVariable Long chainId,
            @Valid @RequestBody UpdateAcquisitionProcessingChain payload) throws ModuleException {
        return ResponseEntity.ok(toResource(processingService.patchStateAndMode(chainId, payload)));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = CHAIN_PATH)
    @ResourceAccess(description = "Delete a chain", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long chainId) throws ModuleException {
        processingService.scheduleProductDeletion(chainId, Optional.empty(), true);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = START_MANUAL_CHAIN_PATH)
    @ResourceAccess(description = "Start a manual chain", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> startManualChain(@PathVariable Long chainId,
            @RequestParam(name = "session", required = false) Optional<String> session) throws ModuleException {
        return ResponseEntity.ok(toResource(processingService.startManualChain(chainId, session, false)));
    }

    @RequestMapping(method = RequestMethod.GET, value = STOP_CHAIN_PATH)
    @ResourceAccess(description = "Stop a chain", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AcquisitionProcessingChain>> stopChain(@PathVariable Long chainId)
            throws ModuleException {
        return ResponseEntity.ok(toResource(processingService.stopAndCleanChain(chainId)));
    }

    @RequestMapping(method = RequestMethod.GET, value = RELAUNCH_ERRORS_PATH)
    @ResourceAccess(description = "Relaunch errors on acquisition chain", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> relaunchErrors(@RequestParam(name = "chainName") String chainName,
            @RequestParam(name = "session") String session)
            throws ModuleException {
        processingService.relaunchErrors(chainName, session);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = CHAIN_SESSION_PRODUCTS_PATH)
    @ResourceAccess(description = "Delete products for a given acquisition chain", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> deleteProducts(@RequestParam(name = "chainName") String chainName,
            @RequestParam(name = "session", required = false) String session) throws ModuleException {
        processingService.scheduleProductDeletion(chainName, Optional.ofNullable(session), false);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Override
    public EntityModel<AcquisitionProcessingChain> toResource(AcquisitionProcessingChain element, Object... extras) {
        EntityModel<AcquisitionProcessingChain> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.LIST,
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        if (!processingService.isDeletionPending(element)) {
            resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(AcquisitionProcessingChain.class));
            if (AcquisitionProcessingChainMode.MANUAL.equals(element.getMode()) && !element.isLocked()
                    && element.isActive()) {
                resourceService.addLink(resource, this.getClass(), "startManualChain", LinkRelation.of("start"),
                                        MethodParamFactory.build(Long.class, element.getId()),
                                        MethodParamFactory.build(Optional.class));
            }
            if (!element.isActive()) {
                resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                                        MethodParamFactory.build(Long.class, element.getId()));
            }
            resourceService.addLink(resource, this.getClass(), "stopChain", LinkRelation.of("stop"),
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "updateStateAndMode", LinkRelation.of("patch"),
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(UpdateAcquisitionProcessingChain.class));
        }
        return resource;
    }
}
