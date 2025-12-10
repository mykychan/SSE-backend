package it.unisa.c02.rently.rently_application.business.gestioneNoleggio.service;

import it.unisa.c02.rently.rently_application.data.dao.GestioneNoleggioDAO;
import it.unisa.c02.rently.rently_application.data.model.Annuncio;
import it.unisa.c02.rently.rently_application.data.model.Noleggio;
import it.unisa.c02.rently.rently_application.data.model.Utente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

/**
 * Implementazione del servizio di gestione dei noleggi.
 * Questa classe fornisce implementazioni concrete per i metodi dichiarati nell'interfaccia GestioneNoleggioService.
 */
@Service
@RequiredArgsConstructor
public class GestioneNoleggioServiceImpl implements GestioneNoleggioService {

    /**
     * Istanza di GestioneNoleggioDAO utilizzata per l'accesso ai dati dei noleggi.
     */
    private final GestioneNoleggioDAO noleggioDAO;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> getNoleggiByNoleggiante(final Utente noleggiante) {
        return noleggioDAO.findByNoleggiante(noleggiante);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> getNoleggiByNoleggiatore(final Utente noleggiatore) {
        return noleggioDAO.findByNoleggiatore(noleggiatore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> getRichiesteByNoleggiante(final Utente noleggiante) {
        return noleggioDAO.findRichiesteByNoleggiante(noleggiante);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> getRichiesteByNoleggiatore(final Utente noleggiatore) {
        return noleggioDAO.findRichiesteByNoleggiatore(noleggiatore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Noleggio addNoleggio(final Noleggio noleggio) {
        return noleggioDAO.save(noleggio);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNoleggio(final Noleggio noleggio) {
        noleggioDAO.deleteById(noleggio.getId());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Noleggio updateStatoNoleggio(final Noleggio noleggio) {
        return noleggioDAO.save(noleggio);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> checkDisponibilita(final Annuncio annuncio, final Date inizio, final Date fine) {
        return noleggioDAO.checkDisponibilita(annuncio, inizio, fine);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Noleggio getNoleggio(final long id) {
        return noleggioDAO.findById(id).orElse(null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> checkFineNoleggio(final Date dateNow) {
        return noleggioDAO.checkFineNoleggio(dateNow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Noleggio> findRichieste() {

        return noleggioDAO.findRichieste();
    }

}
