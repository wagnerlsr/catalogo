//package br.com.carrefour.catalogo.vtex.listener;
//
//import br.com.carrefour.catalogo.core.model.Similar;
//import br.com.carrefour.catalogo.core.repository.SimilarRepository;
//import com.google.gson.Gson;
//import lombok.RequiredArgsConstructor;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.jms.annotation.JmsListener;
//import org.springframework.stereotype.Component;
//
//
//@Component
//@RequiredArgsConstructor
//public class VTexListener {
//
//    private final SimilarRepository similarRepository;
//    private final Logger logger = LogManager.getLogger();
//    private long total = 0;
//    private long timer1 = System.currentTimeMillis();
//
//
//    @JmsListener(destination = "carrefour_vtex_queue", containerFactory = "defaultFactory")
//    public void receiveMessage(String similar) {
//        total++;
//
//        try {
//            Similar s = (new Gson()).fromJson(similar, Similar.class);
//
//            if (total >= 100) {
//                logger.info(String.format(">>>> VTexListener >>>>> %d", (System.currentTimeMillis()-timer1)/1000));
//                timer1 = System.currentTimeMillis();
//                total = 0;
//            }
//
//            similarRepository.save(s);
//        } catch (Exception e) {
//            logger.error(String.format("Error VTexListener [ %s ] [ %s ]", e.getMessage(), similar));
//        }
//    }
//
//}
