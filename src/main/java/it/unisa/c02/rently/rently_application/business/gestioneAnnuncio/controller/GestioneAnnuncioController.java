package it.unisa.c02.rently.rently_application.business.gestioneAnnuncio.controller;

import it.unisa.c02.rently.rently_application.business.gestioneAnnuncio.service.GestioneAnnuncioService;
import it.unisa.c02.rently.rently_application.business.gestioneAreaPersonale.service.GestioneAreaPersonaleService;
import it.unisa.c02.rently.rently_application.commons.services.regexService.RegexTester;
import it.unisa.c02.rently.rently_application.commons.services.responseService.ResponseService;
import it.unisa.c02.rently.rently_application.commons.services.storageService.FilesStorageService;
import it.unisa.c02.rently.rently_application.data.dto.AnnuncioDTO;
import it.unisa.c02.rently.rently_application.data.dto.ResponseDTO;
import it.unisa.c02.rently.rently_application.data.model.Annuncio;
import it.unisa.c02.rently.rently_application.data.model.Utente;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/annuncio")
@CrossOrigin(origins = {"*"}, methods = {
        RequestMethod.OPTIONS,
        RequestMethod.GET,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.POST
})
public class GestioneAnnuncioController {

    private final FilesStorageService storageService;
    private final ResponseService responseService;
    private final GestioneAnnuncioService gestioneAnnuncioService;
    private final GestioneAreaPersonaleService gestioneAreaPersonaleService;
    private final HttpServletRequest httpServletRequest;
    private final ResourceLoader resourceLoader;

    private static final String uploadsPath = "annunci";

    @Value("${uploads.path}")
    private String uploadPath;

    @GetMapping("/visualizza-annuncio")
    public ResponseEntity<String> getAnnuncio(@RequestParam final long id) {
        try {
            final Annuncio annuncio = gestioneAnnuncioService.getAnnuncio(id).orElse(null);
            final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(annuncio);

            final String serverAddress = String.format(
                    "%s://%s:%d",
                    httpServletRequest.getScheme(),
                    httpServletRequest.getServerName(),
                    httpServletRequest.getServerPort()
            );

            item.setServerImage(annuncio, serverAddress);
            return responseService.Ok(item);

        } catch (Exception ex) {
            return responseService.InternalError();
        }
    }

    @GetMapping("/visualizza-annunci-utente")
    public ResponseEntity<String> getAnnunciUtente(@RequestParam final long id) {
        try {
            final Utente u = gestioneAreaPersonaleService.getDatiPrivati(id);
            final List<Annuncio> annunci = gestioneAnnuncioService.findAllByUtente(u);
            final List<AnnuncioDTO> list = new ArrayList<>();

            final String serverAddress = String.format(
                    "%s://%s:%d",
                    httpServletRequest.getScheme(),
                    httpServletRequest.getServerName(),
                    httpServletRequest.getServerPort()
            );

            for (final Annuncio a : annunci) {
                final AnnuncioDTO item = new AnnuncioDTO().convertFromModel(a);
                item.setServerImage(a, serverAddress);
                list.add(item);
            }

            return responseService.Ok(list);
        } catch (Exception ex) {
            return responseService.InternalError();
        }
    }

    @PostMapping(value = "aggiungi-annuncio")
    public ResponseEntity<String> addAnnuncio(@ModelAttribute("model") final AnnuncioDTO model,
                                              @RequestParam("image") final MultipartFile image) {

        try {
            final ResponseDTO message = new ResponseDTO();
            message.message = "Dati inseriti non validi";

            final HashMap<String, String> tester = new HashMap<>();
            tester.put(model.getDescrizione(), "^[\\sa-zA-Z0-9.,:;'-èéòàùì]{1,1023}$");
            tester.put(model.getStrada(), "^[\\sa-zA-Z0-9.,:;'-èéòàùì]+$");
            tester.put(model.getCap(), "^[0-9]{5}$");
            tester.put(model.getNome(), "^[\\sa-zA-Z0-9.,'èéòàùì]{1,100}$");
            tester.put(model.getPrezzo().toString(), "^[0-9]{1,10}[.,][0-9]{2}$");

            final RegexTester regexTester = new RegexTester();
            if (!regexTester.toTest(tester)) {
                return responseService.InternalError(message);
            }

            final Annuncio item = new Annuncio();
            item.setNome(model.getNome());
            item.setStrada(model.getStrada());
            item.setCitta(model.getCitta());
            item.setCap(model.getCap());
            item.setDescrizione(model.getDescrizione());
            item.setPrezzo(model.getPrezzo());
            item.setCategoria(Annuncio.EnumCategoria.valueOf(model.getCategoria().toUpperCase()));
            item.setCondizione(Annuncio.EnumCondizione.valueOf(model.getCondizione().toUpperCase()));
            item.setDataFine(Date.valueOf(model.getDataFine()));

            final Utente user = gestioneAreaPersonaleService.getDatiPrivati(model.getIdUtente());
            if (user != null)
                item.setUtente(user);

            final Annuncio newItem = gestioneAnnuncioService.addAnnuncio(item);

            final String basePath = uploadPath + "annunci/" + newItem.getId() + "/";
            storageService.init(basePath);

            String fileName = storageService.generateRandomFileName();
            final String extension = image.getOriginalFilename()
                    .substring(image.getOriginalFilename().lastIndexOf('.') + 1);
            fileName = fileName + "." + extension;

            storageService.save(image, fileName);
            newItem.setImmagine(fileName);

            gestioneAnnuncioService.updateAnnuncio(newItem);

            final AnnuncioDTO annuncioDto = new AnnuncioDTO().convertFromModel(newItem);
            return responseService.Ok(annuncioDto);

        } catch (Exception ex) {
            return responseService.InternalError();
        }
    }

    @PostMapping(value = "modifica-annuncio")
    public ResponseEntity<String> modifyAnnuncio(@ModelAttribute("model") final AnnuncioDTO model,
                                                 @RequestParam(value = "image", required = false) final MultipartFile image) {

        try {
            final Annuncio item = gestioneAnnuncioService.getAnnuncio(model.getId()).orElse(null);
            if (item == null)
                return responseService.InternalError();

            item.setNome(model.getNome());
            item.setStrada(model.getStrada());
            item.setCitta(model.getCitta());
            item.setCap(model.getCap());
            item.setDescrizione(model.getDescrizione());
            item.setPrezzo(model.getPrezzo());
            item.setCategoria(Annuncio.EnumCategoria.valueOf(model.getCategoria().toUpperCase()));
            item.setCondizione(Annuncio.EnumCondizione.valueOf(model.getCondizione().toUpperCase()));
            item.setDataFine(Date.valueOf(model.getDataFine()));

            final Utente user = gestioneAreaPersonaleService.getDatiPrivati(model.getIdUtente());
            if (user != null)
                item.setUtente(user);

            final Annuncio newItem = gestioneAnnuncioService.updateAnnuncio(item);

            if (image != null) {
                final String basePath = uploadPath + "annunci/" + newItem.getId() + "/";
                storageService.init(basePath);

                String fileName = storageService.generateRandomFileName();
                final String extension = image.getOriginalFilename()
                        .substring(image.getOriginalFilename().lastIndexOf('.') + 1);
                fileName = fileName + "." + extension;

                storageService.deleteAll();
                storageService.save(image, fileName);

                newItem.setImmagine(fileName);
                gestioneAnnuncioService.updateAnnuncio(newItem);
            }

            final AnnuncioDTO annuncioDto = new AnnuncioDTO().convertFromModel(newItem);
            return responseService.Ok(annuncioDto);

        } catch (Exception ex) {
            return responseService.InternalError();
        }
    }

    @GetMapping("/delete-annuncio")
    public ResponseEntity<String> deleteAnnuncio(@RequestParam final long id) {
        try {
            gestioneAnnuncioService.deleteAnnuncio(id);
            return responseService.Ok();
        } catch (Exception ex) {
            return responseService.InternalError();
        }
    }
}
