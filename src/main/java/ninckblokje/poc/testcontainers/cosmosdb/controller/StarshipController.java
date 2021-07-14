/*
 * Copyright (c) 2021, ninckblokje
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ninckblokje.poc.testcontainers.cosmosdb.controller;

import ninckblokje.poc.testcontainers.cosmosdb.model.Franchise;
import ninckblokje.poc.testcontainers.cosmosdb.model.Starship;
import ninckblokje.poc.testcontainers.cosmosdb.repository.StarshipRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/starship")
public class StarshipController {

    private final StarshipRepository starshipRepository;

    public StarshipController(StarshipRepository starshipRepository) {
        this.starshipRepository = starshipRepository;
    }

    @GetMapping
    public Flux<Starship> getAllStarships() {
        return starshipRepository.findAll();
    }

    @GetMapping("/{franchise}")
    public Flux<Starship> getAllStarshipsByFranchise(@PathVariable("franchise") Franchise franchise) {
        return starshipRepository.findAllByFranchise(franchise);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public Mono<Starship> addStarship(@RequestBody @Valid Starship starship) {
        return starshipRepository.save(starship);
    }

    @GetMapping("/classNames")
    public Flux<String> getAllClassNames() {
        return starshipRepository.findAll()
                .map(Starship::getClassName);
    }
}
