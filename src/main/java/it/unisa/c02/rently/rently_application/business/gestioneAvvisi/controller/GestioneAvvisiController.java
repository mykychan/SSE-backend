package it.unisa.c02.rently.rently_application.business.gestioneAvvisi.controller;
import it.unisa.c02.rently.rently_application.business.gestioneAnnuncio.service.GestioneAnnuncioService;
import it.unisa.c02.rently.rently_application.business.gestioneAreaPersonale.service.GestioneAreaPersonaleService;
import it.unisa.c02.rently.rently_application.business.gestioneAvvisi.service.GestioneAvvisiService;
import it.unisa.c02.rently.rently_application.business.gestioneNoleggio.service.GestioneNoleggioService;
import it.unisa.c02.rently.rently_application.commons.mail.EmailService;
import it.unisa.c02.rently.rently_application.commons.services.regexService.RegexTester;
import it.unisa.c02.rently.rently_application.commons.services.responseService.ResponseService;
import it.unisa.c02.rently.rently_application.data.dto.ResponseDTO;
import it.unisa.c02.rently.rently_application.data.dto.SegnalazioneDTO;
import it.unisa.c02.rently.rently_application.data.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;


/**
 * Questa classe gestisce le richieste relative alle segnalazioni e alle notifiche attraverso i servizi forniti da
 * GestioneAvvisiService, GestioneAreaPersonaleService, GestioneAnnuncioService, GestioneNoleggioService,
 * EmailService e ResponseService.
 * Fornisce endpoint RESTful per aggiungere segnalazioni e inviare notifiche relative ai noleggi.
 * Le risposte sono gestite utilizzando il servizio ResponseService per costruire risposte standardizzate in formato JSON.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avvisi")
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
public class GestioneAvvisiController {

    /**
     * Service per effettuare le operazioni di persistenza.
     */
    private final GestioneAvvisiService avvisiService;

    /**
     * Service per effettuare le operazioni di persistenza.
     */
    private final GestioneAreaPersonaleService areaPersonaleService;

    /**
     * Service per la gestione delle risposte alle richieste.
     */
    private final ResponseService responseService;

    /**
     * Service per la gestione dell'invio delle mail.
     */
    private final EmailService emailService;

    /**
     * Service per effettuare le operazioni di persistenza.
     */
    private final GestioneAnnuncioService annuncioService;

    /**
     * Service per effettuare le operazioni di persistenza.
     */
    private final GestioneNoleggioService noleggioService;


    /**
     * Endpoint per aggiungere una segnalazione.
     *
     * @param segnalazioneDTO Dati della segnalazione da aggiungere.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @PostMapping("/aggiungi-segnalazione")
    public ResponseEntity<String> aggiungiSegnalazione(@RequestBody final SegnalazioneDTO segnalazioneDTO) {

        try {
            //prendo l'utente dalla sessione con @AuthenticationPrincipal UserDetails userDetails
            //controllo utente sessione
            final ResponseDTO message = new ResponseDTO();
            message.message = "Il contenuto inserito non rispetta la regex";

            final HashMap<String, String> tester = new HashMap<>();
            tester.put(segnalazioneDTO.getContenuto(), "^[\\sa-zA-Z0-9.,:;'-èéòàùì]{1,255}$");

            final RegexTester regexTester = new RegexTester();
            if (!regexTester.toTest(tester)) {
                return responseService.InternalError(message);
            }

            final Segnalazione segnalazione = new Segnalazione();
            segnalazione.setContenuto(segnalazioneDTO.getContenuto());
            segnalazione.setTipo(Segnalazione.EnumTipo.valueOf(segnalazioneDTO.getTipo()));
            final Utente user = areaPersonaleService.getDatiPrivati(segnalazioneDTO.getSegnalatore());
            if (user != null) {
                segnalazione.setSegnalatore(user);
            }
            final Segnalazione nuova = avvisiService.addSegnalazione(segnalazione);

            if (nuova.getSegnalatore() != null) {
                return responseService.Ok(nuova);
            } else
                return responseService.InternalError();

        } catch (final Exception e) {
            return responseService.InternalError();
        }
    }


    /**
     * Endpoint per inviare una notifica al noleggiatore relativa all'arrivo di una richiesta noleggio.
     *
     * @param idNoleggio id del noleggio relativo alla notifica.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @GetMapping("/notifica-arrivo-richiesta-noleggio")
    public ResponseEntity<String> notificaArrivoRichiestaNoleggio(@RequestParam final long idNoleggio){
        try{
            final Noleggio noleggio = noleggioService.getNoleggio(idNoleggio);
            if(noleggio== null){
                return responseService.InternalError();
            }
            final Utente noleggiatore= noleggio.getNoleggiatore();
            final Annuncio annuncio = annuncioService.getAnnuncio(noleggio.getAnnuncio().getId()).orElse(null);
            if(noleggiatore!= null && annuncio!=null){
                final String subject = "Nuova richiesta di noleggio";
                final String text = "Salve "+noleggiatore.getUsername()+",\nhai appena ricevuto una nuova richiesta di noleggio da parte di "+noleggio.getNoleggiante().getUsername()+" per"+
                        " il tuo annuncio "+annuncio.getNome()+".\nAccedi a Rently e controlla le tue richieste per saperne di più!";
                emailService.sendEmail(noleggiatore.getEmail(), subject,text);
                return responseService.Ok();
            }else
                return responseService.InternalError();
        }catch(final Exception e){
            return responseService.InternalError();
        }
    }


    /**
     * Endpoint per inviare una notifica al noleggiante relativa all'accettazione di una richiesta noleggio.
     *
     * @param idNoleggio id del noleggio relativo alla notifica.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @GetMapping("/notifica-richiesta-noleggio-accettata")
    public ResponseEntity<String> notificaRichiestaNoleggioAccettata(@RequestParam final long idNoleggio){
        try{
            final Noleggio noleggio = noleggioService.getNoleggio(idNoleggio);
            if(noleggio== null){
                return responseService.InternalError();
            }
            final Utente noleggiante= noleggio.getNoleggiante();
            final Annuncio annuncio = annuncioService.getAnnuncio(noleggio.getAnnuncio().getId()).orElse(null);
            if(noleggiante!= null && annuncio!=null){
                final String subject = "Richiesta di noleggio accettata";
                final String text = "Salve "+noleggiante.getUsername()+",\nla tua richiesta di noloeggio per l'annuncio "+annuncio.getNome()+" è stata appena accettata!." +
                        "\nAccedi a Rently per procedere con il pagamento e dare inizio al periodo di noleggio!";

                emailService.sendEmail(noleggiante.getEmail(), subject,text);
                return responseService.Ok();
            }else
                return responseService.InternalError();
        }catch(final Exception e){
            return responseService.InternalError();
        }
    }


    /**
     * Endpoint per inviare una notifica al noleggiante relativa al rifiuto di una richiesta noleggio.
     *
     * @param idNoleggio id del noleggio relativo alla notifica.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @GetMapping("/notifica-richiesta-noleggio-rifiutata")
    public ResponseEntity<String> notificaRichiestaNoleggioRifiutata(@RequestParam final long idNoleggio){
        try{
            final Noleggio noleggio = noleggioService.getNoleggio(idNoleggio);
            if(noleggio== null){
                return responseService.InternalError();
            }
            final Utente noleggiante= noleggio.getNoleggiante();
            final Annuncio annuncio = annuncioService.getAnnuncio(noleggio.getAnnuncio().getId()).orElse(null);
            if(noleggiante!= null && annuncio!=null){
                final String subject = "Richiesta di noleggio accettata";
                final String text = "Salve "+noleggiante.getUsername()+",\nla tua richiesta di noloeggio per l'annuncio "+annuncio.getNome()+" è stata rifiutata." +
                        "\nAccedi a Rently e controlla le tue richieste per saperne di più.";

                emailService.sendEmail(noleggiante.getEmail(), subject,text);
                return responseService.Ok();
            }else
                return responseService.InternalError();
        }catch(final Exception e){
            return responseService.InternalError();
        }
    }


    /**
     * Endpoint per inviare una notifica al noleggiante e al noleggiatore relativa all'inizio di un noleggio.
     *
     * @param idNoleggio id del noleggio relativo alla notifica.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @GetMapping("/notifica-inizio-noleggio")
    public ResponseEntity<String> notificaInizioNoleggio(@RequestParam final long idNoleggio){
        try{
            final Noleggio noleggio = noleggioService.getNoleggio(idNoleggio);
            if(noleggio== null){
                return responseService.InternalError();
            }
            final Utente noleggiante= noleggio.getNoleggiante();
            final Utente noleggiatore= noleggio.getNoleggiatore();
            final Annuncio annuncio = annuncioService.getAnnuncio(noleggio.getAnnuncio().getId()).orElse(null);
            if(noleggiante!= null && noleggiatore!= null && annuncio!=null){
                final String subject = "Inizio periodo di noleggio";
                final String textNoleggiante = "Salve "+noleggiante.getUsername()+",\nil periodo di noleggio per l'oggetto "+annuncio.getNome()+" è appena iniziato." +
                        "\nRicordiamo che la scadenza del periodo è prevista per il giorno "+noleggio.getDataFine()+". Accedi a Rently e controlla i tuoi noleggi per saperne di più.";
                final String textNoleggiatore = "Salve "+noleggiatore.getUsername()+",\nil periodo di noleggio per l'oggetto "+annuncio.getNome()+" richiesto da "+noleggiante.getUsername()+" è appena iniziato." +
                        "\nRicordiamo che la scadenza del periodo è prevista per il giorno "+noleggio.getDataFine()+". Accedi a Rently e controlla i tuoi noleggi per saperne di più.";

                emailService.sendEmail(noleggiante.getEmail(), subject,textNoleggiante);
                emailService.sendEmail(noleggiatore.getEmail(), subject,textNoleggiatore);
                return responseService.Ok();
            }else
                return responseService.InternalError();
        }catch(final Exception e){
            return responseService.InternalError();
        }
    }


    /**
     * Endpoint per inviare una notifica al noleggiante e al noleggiatore relativa alla fine di un noleggio.
     *
     * @param idNoleggio id del noleggio relativo alla notifica.
     * @return ResponseEntity contenente l'esito dell'operazione.
     */
    @GetMapping("/notifica-fine-noleggio")
    public ResponseEntity<String> notificaFineNoleggio(@RequestParam final long idNoleggio){
        try{
            final Noleggio noleggio = noleggioService.getNoleggio(idNoleggio);
            if(noleggio== null){
                return responseService.InternalError();
            }
            final Utente noleggiante= noleggio.getNoleggiante();
            final Utente noleggiatore= noleggio.getNoleggiatore();
            final Annuncio annuncio = annuncioService.getAnnuncio(noleggio.getAnnuncio().getId()).orElse(null);
            if(noleggiante!= null && noleggiatore!= null && annuncio!=null){
                final String subject = "Fine periodo di noleggio";
                final String textNoleggiante = "Salve "+noleggiante.getUsername()+",\nil periodo di noleggio per l'oggetto "+annuncio.getNome()+" è terminato." +
                        "\nTi invitiamo a condividere la tua esperienza lasciando una valutazione sul proprietario e sull'oggetto noleggiato. Accedi a Rently e controlla i tuoi noleggi per saperne di più.";
                final String textNoleggiatore = "Salve "+noleggiatore.getUsername()+",\nil periodo di noleggio per l'oggetto "+annuncio.getNome()+" richiesto da "+noleggiante.getUsername()+" è terminato." +
                        "\nTi invitiamo a condividere la tua esperienza lasciando una valutazione su "+noleggiante.getUsername()+". Accedi a Rently e controlla i tuoi noleggi per saperne di più.";

                emailService.sendEmail(noleggiante.getEmail(), subject,textNoleggiante);
                emailService.sendEmail(noleggiatore.getEmail(), subject,textNoleggiatore);
                return responseService.Ok();
            }else
                return responseService.InternalError();
        }catch(final Exception e){
            return responseService.InternalError();
        }
    }

}
