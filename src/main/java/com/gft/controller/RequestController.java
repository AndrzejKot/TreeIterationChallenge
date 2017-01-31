package com.gft.controller;

import com.gft.iterable.IterableNode;
import com.gft.node.DirNode;
import com.gft.watcher.DirWatcher;
import lombok.extern.log4j.Log4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

@Log4j
@RestController
class RequestController {

    public RequestController() {

    }

    private Path root;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Subscriber<Path> initSubscriber() {
        return new Subscriber<Path>() {
            @Override
            public void onCompleted() {
                //Watcher should never end.
            }

            @Override
            public void onError(Throwable e) {
                log.error(e);
            }

            @Override
            public void onNext(Path path) {
                messagingTemplate.convertAndSend("/topic/broadcast", path.toString());
            }
        };
    }

    @RequestMapping(value = "/addFile", method= RequestMethod.GET)
    String addFile(@RequestParam(value="name") String name) throws IOException {
        final Path path;
        System.out.println(root);
        System.out.println(root + File.separator + name);
//        if (root != null) {
            path = Paths.get(root + File.separator + name);
//        } else {
//            path = Paths.get(File.separator + name);
//        }
        System.out.println(path);
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            log.debug(e);
            return "File: " + name + " already exists! Try with a different name.";
        }
        return "File: " + name + " created successfully.";
    }

    @RequestMapping(value = "/init", method= RequestMethod.GET)
    List<String> getInitialDirStructure(@RequestParam(value="root", required=false,
            defaultValue="C:\\Users\\ankt\\Desktop\\challenge") String dir) {
        root = Paths.get(dir);
        val subscriber = initSubscriber();
        DirWatcher.watch(root).subscribeOn(Schedulers.newThread()).subscribe(subscriber);
        val paths = new LinkedList<String>();
        for(Path element : new IterableNode<Path>(new DirNode(root))) {
            paths.add(element.toString());
        }
        return paths;
    }
}