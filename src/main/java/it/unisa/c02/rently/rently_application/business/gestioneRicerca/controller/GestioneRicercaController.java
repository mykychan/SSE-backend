package it.unisa.c02.rently.rently_application.business.gestioneRicerca.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;
import it.unisa.c02.rently.rently_application.business.gestioneRicerca.service.GestioneRicercaService;
import it.unisa.c02.rently.rently_application.commons.jsonHelper.JsonHelper;
import it.unisa.c02.rently.rently_application.commons.services.responseService.ResponseService;
import it.unisa.c02.rently.rently_application.commons.services.storageService.FilesStorageService;
import it.unisa.c02.rently.rently_application.data.dto.AnnuncioDTO;
import it.unisa.c02.rently.rently_application.data.model.Annuncio;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Questa classe gestisce le richieste di ricerca di annunci attraverso i servizi offerti da GestioneRicercaService.
 * Fornisce endpoint RESTful per cercare annunci in base a diversi criteri.
 * Le risposte vengono restituite nel formato JSON attraverso ResponseEntity<String>, utilizzando le funzionalità di
 * ResponseService per gestire la costruzione delle risposte standardizzate.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ricerca")
@CrossOrigin(
        origins = {
                "*",
        },
        methods = {
                RequestMethod.OPTIONS,
                RequestMethod.GET,
                RequestMethod.PUT,
                RequestMethod.DELETE,
                RequestMethod.POST
        })
public class GestioneRicercaController {

    /**
     * Service per effettuare le operazioni di persistenza.
     */
    private final GestioneRicercaService ricercaService;

    /**
     * Service per la gestione delle risposte alle richieste.
     */
    private final ResponseService responseService;

    /**
     * Oggetto HttpServletRequest per ottenere informazioni sulla richiesta HTTP corrente.
     */
    private final HttpServletRequest httpServletRequest;

    /**
     * Restituisce una lista di annunci che corrispondono alla categoria specificata.
     *
     * @param categoria Categoria degli annunci da cercare.
     * @return ResponseEntity contenente la lista di annunci nel formato JSON.
     */
    @GetMapping("/categoria")
    public ResponseEntity<String>  searchByCategoria(@RequestParam final String categoria) {
        final List<Annuncio> annunci = ricercaService.searchByCategoria(categoria);
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();
        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            list.add(item);
        }
        return responseService.Ok(list);
    }

    /**
     * Restituisce una lista di annunci che corrispondono alla condizione specificata.
     *
     * @param condizione Condizione degli annunci da cercare.
     * @return ResponseEntity contenente la lista di annunci nel formato JSON.
     */
    @GetMapping("/condizione")
    public ResponseEntity<String> searchByCondizione(@RequestParam final String condizione) {
        final List<Annuncio> annunci = ricercaService.searchByCondizione(condizione);
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();
        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            list.add(item);
        }
        return responseService.Ok(list);
    }

    /**
     * Restituisce una lista di annunci pubblicati tra le date specificate.
     *
     * @param inizio Data di inizio periodo di ricerca.
     * @param fine Data di fine periodo di ricerca.
     * @return ResponseEntity contenente la lista di annunci nel formato JSON.
     */
    @GetMapping("/data")
    public ResponseEntity<String> searchByData(@RequestParam final Date inizio, @RequestParam final Date fine) {
        final List<Annuncio> annunci = ricercaService.searchByData(inizio, fine);
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();
        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            list.add(item);
        }
        return responseService.Ok(list);
    }

    /**
     * Restituisce una lista di annunci che contengono la descrizione specificata.
     *
     * @param descrizione Descrizione da cercare negli annunci.
     * @return ResponseEntity contenente la lista di annunci nel formato JSON.
     */
    @GetMapping("/descrizione")
    public ResponseEntity<String> searchByDescrizione(@RequestParam final String descrizione) {
        final List<Annuncio> annunci =  ricercaService.searchByDescrizione(descrizione);
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();
        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            list.add(item);
        }
        return responseService.Ok(list);
    }

    /**
     * Restituisce una lista di tutti gli annunci presenti sulla piattaforma.
     *
     * @return ResponseEntity contenente la lista di annunci nel formato JSON.
     */
    @GetMapping("/all")
    public ResponseEntity<String> searchAll() {
        final List<Annuncio> annunci =  ricercaService.searchAll();
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();

        final String serverAddress = String.format(
                "%s://%s:%d",
                httpServletRequest.getScheme(),
                httpServletRequest.getServerName(),
                httpServletRequest.getServerPort());

        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            item.setServerImage(a, serverAddress);
            list.add(item);
        }

        return responseService.Ok(list);
    }

    /**
     * Restituisce una lista di annunci di utenti premium presenti sulla piattaforma.
     *
     * @return ResponseEntity contenente la lista di annunci di utenti premium nel formato JSON.
     */
    @GetMapping("/premium")
    public ResponseEntity<String> searchAnnunciPremium() {
        final List<Annuncio> annunci =  ricercaService.searchAnnunciPremium();
        final List<AnnuncioDTO> list = new ArrayList<AnnuncioDTO>();

        final String serverAddress = String.format(
                "%s://%s:%d",
                httpServletRequest.getScheme(),
                httpServletRequest.getServerName(),
                httpServletRequest.getServerPort());

        for (final Annuncio a: annunci) {
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
            item.setServerImage(a, serverAddress);
            list.add(item);
        }

        return responseService.Ok(list);
    }
}

