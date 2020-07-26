package i5.las2peer.services.codeGenerationService.generators;

import com.google.common.collect.ImmutableMap;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import i5.las2peer.api.Context;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.codeGenerationService.CodeGenerationService;
import i5.las2peer.services.codeGenerationService.adapters.BaseGitHostAdapter;
import i5.las2peer.services.codeGenerationService.exception.GitHostException;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.Event;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.Function;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.HtmlElement;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCCall;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.IWCResponse;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.InputParameter;
import i5.las2peer.services.codeGenerationService.models.frontendComponent.MicroserviceCall;
import i5.las2peer.services.codeGenerationService.models.traceModel.FileTraceModel;
import i5.las2peer.services.codeGenerationService.models.traceModel.TraceModel;
import i5.las2peer.services.codeGenerationService.templateEngine.InitialGenerationStrategy;
import i5.las2peer.services.codeGenerationService.templateEngine.Template;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateEngine;
import i5.las2peer.services.codeGenerationService.templateEngine.TemplateStrategy;

/**
 * Generates frontend component source code from passed on
 * {@link i5.las2peer.services.codeGenerationService.models.frontendComponent.FrontendComponent}
 * models.
 */
public class FrontendComponentGenerator extends Generator {
    private static final L2pLogger logger =
            L2pLogger.getInstance(ApplicationGenerator.class.getName());


    public static String getRepositoryName(FrontendComponent frontendComponent) {
        return "frontendComponent-" + frontendComponent.getVersionedModelId();
    }

    /**
     * Creates source code from a CAE frontend component model and pushes it to GitHub.
     *
     * @param frontendComponent the frontend component model
     * @param versionTag String which should be used as the tag when commiting. May be null.
     * @return Commit sha identifier
     * @throws GitHostException thrown if anything goes wrong during this process. Wraps around all
     *                          other exceptions and prints their message.
     */
    public static String createSourceCode(FrontendComponent frontendComponent, BaseGitHostAdapter gitAdapter,
    		String commitMessage, String versionTag, boolean forcePush) throws GitHostException {
        // variables to be closed in the final block
        Repository frontendComponentRepository = null;
        TreeWalk treeWalk = null;

        // helper variables
        // variables holding content to be modified and added to repository later
        String widget = null;
        String applicationScript = null;
        String las2peerWidgetLibrary = null;
        String style = null;
        String readMe = null;
        BufferedImage logo = null;
        String htmlElementTemplate = null;
        String wireframeElementTemplate = null;
        String functionTemplate = null;
        String microserviceCallTemplate = null;
        String iwcResponseTemplate = null;
        String eventTemplate = null;
        String yjsImports = null;
        String polymerLibImport = null;
        String polymerElementImport = null;
        String yjs = null;
        String yText = null;
        String yWebsockets = null;
        String yArray = null;
        String yMemory = null;
        String iwc = null;
        String webComponents = null;
        String yjsInit = null;
        String guidances = null;

        try {
            PersonIdent caeUser = new PersonIdent(gitAdapter.getGitUser(), gitAdapter.getGitUserMail());
            String repositoryName = getRepositoryName(frontendComponent);
            String componentName = frontendComponent.getName();
            frontendComponentRepository = generateNewRepository(repositoryName, gitAdapter);


            try {
                // now load the TreeWalk containing the template repository content
                treeWalk = getTemplateRepositoryContent(gitAdapter);
                treeWalk.setFilter(PathFilter.create("frontend/"));
                ObjectReader reader = treeWalk.getObjectReader();
                // walk through the tree and retrieve the needed templates
                while (treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = reader.open(objectId);

                    switch (treeWalk.getNameString()) {
                        case "index.html":
                            widget = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericHtmlElement.txt":
                            htmlElementTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericWireframeElement.txt":
                            wireframeElementTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericEvent.txt":
                            eventTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "applicationScript.js":
                            applicationScript = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "guidances.json":
                            guidances = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericFunction.txt":
                            functionTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericMicroserviceCall.txt":
                            microserviceCallTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "genericIWCResponse.txt":
                            iwcResponseTemplate = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "yjs-imports.txt":
                            yjsImports = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "polymer-element-import.txt":
                            polymerElementImport = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "polymer-lib-import.txt":
                            polymerLibImport = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "las2peerWidgetLibrary.js":
                            las2peerWidgetLibrary = new String(loader.getBytes(), "UTF-8");
                            las2peerWidgetLibrary = replaceExactMatch(las2peerWidgetLibrary,
                                    "%%oidcProvider%%",
                                    ((CodeGenerationService) Context.getCurrent().getService()).getOidcProvider());
                            break;
                        case "webcomponents-lite.min.js":
                            webComponents = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "iwc.js":
                            iwc = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "y.js":
                            yjs = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "y-array.js":
                            yArray = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "y-text.js":
                            yText = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "y-websockets-client.js":
                            yWebsockets = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "y-memory.js":
                            yMemory = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "yjsInit.txt":
                            yjsInit = new String(loader.getBytes(), "UTF-8");
                            break;
                        // case "yjsBindCode.txt":
                        // yjsBindCode = new String(loader.getBytes(), "UTF-8");
                        // break;
                        // case "yjsSyncedTemplate.txt":
                        // yjsSyncedCode = new String(loader.getBytes(), "UTF-8");
                        // break;
                        case "style.css":
                            style = new String(loader.getBytes(), "UTF-8");
                            break;
                        case "README.md":
                            readMe = new String(loader.getBytes(), "UTF-8");
                            readMe = readMe.replace("$Repository_Name$", repositoryName);
                            readMe = readMe.replace("$Widget_Name$", componentName);
                            readMe = readMe.replace("$Organization_Name$", gitAdapter.getGitOrganization());
                            break;
                        case "logo_frontend.png":
                            logo = ImageIO.read(loader.openStream());
                            break;
                    }
                }
            } catch (Exception e) {
                logger.printStackTrace(e);
                throw new GitHostException(e.getMessage());
            }
            // the global traceModel
            TraceModel traceModel = new TraceModel();

            FileTraceModel widgetTraceModel = new FileTraceModel(traceModel, "index.html");
            TemplateStrategy strategy = new InitialGenerationStrategy();
            TemplateEngine widgetTemplateEngine = new TemplateEngine(strategy, widgetTraceModel);

            //prepare additional import map
            Map<String, String> imports = ImmutableMap.of(
                    "Yjs", yjsImports,
                    "webComponents", polymerLibImport,
                    "polymerElement", polymerElementImport);

            // add html elements to widget source code
            createWidgetCode(widgetTemplateEngine, widget, wireframeElementTemplate, imports,
                    gitAdapter.getGitOrganization(), repositoryName, frontendComponent);

            // add widget file trace model to gloabl trace model

            traceModel.addFileTraceModel(widgetTraceModel);

            // add functions to application script

            FileTraceModel applicationScriptTraceModel =
                    new FileTraceModel(traceModel, "js/applicationScript.js");
            TemplateStrategy applicationScriptStrategy = new InitialGenerationStrategy();
            TemplateEngine applicationScriptTemplateEngine =
                    new TemplateEngine(applicationScriptStrategy, applicationScriptTraceModel);

            Template applicationTemplate = applicationScriptTemplateEngine.createTemplate(
                    frontendComponent.getWidgetModelId() + ":applicationScript:", applicationScript);

            applicationScriptTemplateEngine.addTemplate(applicationTemplate);

            createApplicationScript(applicationTemplate, functionTemplate, microserviceCallTemplate,
                    iwcResponseTemplate, htmlElementTemplate, frontendComponent);

            // add events to elements
            addEventsToApplicationScript(applicationTemplate, widgetTemplateEngine, eventTemplate,
                    frontendComponent);
            // add (possible) Yjs collaboration stuff
            addYjsCollaboration(applicationTemplate, applicationScriptTemplateEngine, yjsInit,
                    frontendComponent);

            // add applicationscript file trace model to gloabl trace model

            traceModel.addFileTraceModel(applicationScriptTraceModel);

            // add files to new repository

            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "traces/", "tracedFiles.json",
                            traceModel.toJSONObject().toJSONString().replace("\\", ""));

            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository, "",
                    "index.html", widgetTemplateEngine.getContent());
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,
                    "traces/", "index.html.traces", widgetTemplateEngine.toJSONObject().toJSONString());

            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository, "js/",
                    "applicationScript.js", applicationScriptTemplateEngine.getContent());
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,
                    "traces/js/", "applicationScript.js.traces",
                    applicationScriptTemplateEngine.toJSONObject().toJSONString());

            // libraries
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,
                    "js/lib/", "las2peerWidgetLibrary.js", las2peerWidgetLibrary);
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "js/lib/", "iwc.js", iwc);
            // y-is (if needed)
            // if (widgetTemplateEngine.getContent().contains("/js/lib/y.js")) {
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "js/lib/", "webcomponents-lite.min.js", webComponents);
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "js/lib/", "y.js", yjs);
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "js/lib/", "y-array.js", yArray);
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "js/lib/", "y-text.js", yText);
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,"js/lib/", "y-websockets-client.js", yWebsockets);
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,"js/lib/", "y-memory.js", yMemory);
            // }
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "css/", "style.css", style);
            frontendComponentRepository =
                    createTextFileInRepository(frontendComponentRepository, "", "README.md", readMe);
            frontendComponentRepository =
                    createImageFileInRepository(frontendComponentRepository, "img/", "logo.png", logo);
            frontendComponentRepository = createTextFileInRepository(frontendComponentRepository,
                    "traces/", "guidances.json", guidances);

            // commit files
            String commitSha = "";
            try {
                RevCommit commit = Git.wrap(frontendComponentRepository).commit()
                        .setMessage(commitMessage)
                        .setCommitter(caeUser).call();
                
                Ref head = frontendComponentRepository.getAllRefs().get("HEAD");
                commitSha = head.getObjectId().getName();
                
                if(versionTag != null) {
                	Git.wrap(frontendComponentRepository).tag().setObjectId(commit).setName(versionTag).call();	
                }
            } catch (Exception e) {
                logger.printStackTrace(e);
                throw new GitHostException(e.getMessage());
            }

            // push (local) repository content to GitHub repository "gh-pages" branch
            try {
                pushToRemoteRepository(frontendComponentRepository, "master", "gh-pages", gitAdapter, versionTag, forcePush);
            } catch (Exception e) {
                logger.printStackTrace(e);
                throw new GitHostException(e.getMessage());
            }
            return commitSha;

            // close all open resources
        } finally {
            if (frontendComponentRepository != null)
                frontendComponentRepository.close();
            if (treeWalk != null)
                treeWalk.close();
        }
    }

    public static void createSourceCode(FrontendComponent frontendComponent, BaseGitHostAdapter gitAdapter, String commitMessage,
    		String versionTag) throws GitHostException {
        createSourceCode(frontendComponent, gitAdapter, commitMessage, versionTag, false);
    }


    /**
     * Creates the "index.html" code according to a passed frontend component model.
     *
     * @param templateEngine          the template engine used for the code generation
     * @param widgetTemplateFile      the widget template of the widget as a string
     * @param htmlElementTemplateFile the HTML element template as a string
     * @param importTemplateFiles    a text file containing all additional imports to be added
     * @param gitHubOrganization      the organization name (for correct paths)
     * @param repositoryName          the repository's name (for correct paths)
     * @param frontendComponent       a {@link FrontendComponent}
     */
    static void createWidgetCode(TemplateEngine templateEngine, String widgetTemplateFile,
                                        String htmlElementTemplateFile, Map<String,String> importTemplateFiles, String gitHubOrganization,
                                        String repositoryName, FrontendComponent frontendComponent) {

        boolean wasWebComponentLibAdded = false;
        ArrayList<String> polymerElementsToAdd = new ArrayList<>();
        Template widgetTemplate = templateEngine.createTemplate(frontendComponent.getWidgetModelId(), widgetTemplateFile);
        templateEngine.addTemplate(widgetTemplate);

        Map<String, HtmlElement> htmlElementsToAdd = new HashMap<String, HtmlElement>();


        // add all static elements and check for collaborative elements
        for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
            if (element.isStaticElement() && !element.hasParent()) {
                htmlElementsToAdd.put(element.getId(), element);
            }
            if (element.isCollaborativeElement()) {
                // currently, only textareas are supported
                Template importTemplate = templateEngine.createTemplate(widgetTemplate.getId() + ":additionalYjsImports", importTemplateFiles.get("Yjs"));
                widgetTemplate.appendVariableOnce("$Additional_Imports$", importTemplate);
            }
            if(element.getType().equals(HtmlElement.ElementType.CUSTOM)){
                if(!wasWebComponentLibAdded) {
                    Template webComponentLibTemplate = templateEngine.createTemplate(widgetTemplate.getId() + ":additionalWebComponentImport", importTemplateFiles.get("webComponents"));
                    widgetTemplate.appendVariableOnce("$Additional_Imports$", webComponentLibTemplate);
                    wasWebComponentLibAdded = true;
                }
                String elementUrl = element.getAttributeValue("link");
                if(elementUrl != null && elementUrl.length() > 0 && !polymerElementsToAdd.contains(elementUrl)){
                    polymerElementsToAdd.add(elementUrl);
                    Template polymerElementImport = templateEngine.createTemplate(widgetTemplate.getId() + ":"+ element.getId() + "AdditionalPolymerElementImport", importTemplateFiles.get("polymerElement"));
                    polymerElementImport.setVariable("$PolymerElement_URL$", elementUrl);
                    widgetTemplate.appendVariable("$Additional_Imports$", polymerElementImport);
                }
            }
        }

        // add own additional script trace
        templateEngine.addTrace(frontendComponent.getWidgetModelId(), "Own additional scripts",
                widgetTemplate.getSegment()
                        .getChildRecursive(frontendComponent.getWidgetModelId() + ":unprotected[0]"));

        // now, get all "updated", but not "created" elements (since these are there "from the start")
        ArrayList<String> idsToAdd = new ArrayList<String>();
        ArrayList<String> htmlElementsCreated = new ArrayList<String>();
        for (Function function : frontendComponent.getFunctions().values()) {
            idsToAdd.addAll(function.getHtmlElementUpdates());
            htmlElementsCreated.addAll(function.getHtmlElementCreations());
        }
        idsToAdd.removeAll(htmlElementsCreated);
        for (String idToAdd : idsToAdd) {
            htmlElementsToAdd.put(idToAdd, frontendComponent.getHtmlElements().get(idToAdd));
        }
        // now we got all elements that are there from the start on, so add them to the widget code

        for (HtmlElement element : htmlElementsToAdd.values()) {
            Template indent = templateEngine
                    .createTemplate(widgetTemplate.getId() + ":indent:" + element.getModelId(), "\n   ");
            widgetTemplate.appendVariable("$Main_Content$", indent);
            Template elementTemplate =
                    createHtmlElementTemplate(element, htmlElementTemplateFile, widgetTemplate);

            widgetTemplate.appendVariable("$Main_Content$", elementTemplate);
            templateEngine.addTrace(element.getModelId(), "HTML Element", element.getId(),
                    elementTemplate);
        }

        widgetTemplate.setVariable("$Widget_Title$", frontendComponent.getWidgetName());

        //String widgetHome = "http://" + gitHubOrganization + ".github.io/" + repositoryName;
        //String widgetHome = "http://cloud10.dbis.rwth-aachen.de:8088/"+repositoryName;
        String widgetHome = ((CodeGenerationService) Context.getCurrent().getService()).getWidgetHomeBaseURL() + repositoryName;
        widgetTemplate.setVariable("$Widget_Home$", widgetHome);
        widgetTemplate.setVariableIfNotSet("$Main_Content$", "");
        widgetTemplate.setVariableIfNotSet("$Additional_Imports$", "");
    }


    /**
     * Creates a template for an HTML element.
     *
     * @param element                 the element
     * @param htmlElementTemplateFile a template containing the code for an HTML element
     * @param template                The template instance in which the html element should be added
     */
    private static Template createHtmlElementTemplate(HtmlElement element,
                                                      String htmlElementTemplateFile, Template template) {
        String wireframeAttributes = "";
        if(element.isContentEditable())
            htmlElementTemplateFile = htmlElementTemplateFile.replace("$Element_Content$", "-{$Element_Content$}-");

        Template elementTemplate = template.createTemplate(element.getModelId() + ":htmlElement", htmlElementTemplateFile);

        String wireframeGeometry = element.generateCodeForGeometry();
        if(wireframeGeometry.length() > 0)
            elementTemplate.setVariable("$Wireframe_Geometry$", wireframeGeometry);
        else
            elementTemplate.setVariable("$Wireframe_Geometry$", "");


        switch (element.getType()) {
            case CUSTOM:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                String tagName = element.getAttributeValue("name");
                if(tagName.length() < 1)
                    tagName = "polymer-name-missing";
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", tagName);
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Element_Content$", " ");
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Closing_Element$", "</" + tagName + ">");
                break;
            case br:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Element_Content$", " ");
                elementTemplate.setVariable("$Closing_Element$", "");
                break;
            case div:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");

                if(element.hasChildren()){
                    for(HtmlElement child : element.getChildren()){
                        Template tpl = createHtmlElementTemplate(child, htmlElementTemplateFile, template);
                        elementTemplate.appendVariable("$Element_Content$", tpl);
                    }
                }
                else
                    elementTemplate.setVariable("$Element_Content$", " ");
                break;
            case ul:
            case ol:
            case dl:
            case table:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Element_Content$", element.getCodeSample());
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            case textarea:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " resize: none; ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                if (element.getLabel() != null && element.getLabel().length() > 0)
                    elementTemplate.setVariable("$Element_Content$", element.getLabel());
                else
                    elementTemplate.setVariable("$Element_Content$", " ");
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            case video:
            case audio:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Element_Content$", "Your browser does not support the" + element.getType().toString() + " tag.");
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            case p:
            case a:
            case span:
            case button:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                if (element.getLabel() != null && element.getLabel().length() > 0)
                    elementTemplate.setVariable("$Element_Content$", element.getLabel());
                else
                    elementTemplate.setVariable("$Element_Content$", " ");
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            case radio:
            case checkbox:
                elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", "label");
                elementTemplate.setVariable("$Element_Id$", "");
                elementTemplate.setVariable("$Additional_Values$", " ");
                wireframeAttributes = element.generateCodeForAttributes();

                StringBuilder inputTemplate = new StringBuilder();
                inputTemplate.append("<input ").append("$Wireframe_Attributes$")
                        .append(" type=\"").append(element.getType())
                        .append("\" id=\"").append(element.getId()).append("\" ")
                        .append("-{$Additional_Values$}-").append(" >")
                        .append("$Element_Content$");

                Template inputElementTemplate = template.createTemplate(element.getId() + ":htmlElementSub", inputTemplate.toString());
                inputElementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                inputElementTemplate.setVariable("$Additional_Values$", " ");
                inputElementTemplate.setVariable("$Element_Content$", element.getLabel());
                elementTemplate.appendVariable("$Element_Content$", inputElementTemplate);

                elementTemplate.setVariable("$Closing_Element$", "</label>");
                break;
            case img:
            case iframe:
            case input:
                wireframeAttributes = element.generateCodeForAttributes();
                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", "");
                elementTemplate.setVariable("$Additional_Styles$", " ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Element_Content$", " ");
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            case svg:
            case canvas:
                wireframeAttributes = element.generateCodeForAttributes();
                String size= " height=\"" + element.getGeometryAttributeValue("height") + "\""
                        + " width=\"" + element.getGeometryAttributeValue("width") + "\"";

                if (wireframeAttributes.length() > 0)
                    elementTemplate.setVariable("$Wireframe_Attributes$", wireframeAttributes + size);
                else
                    elementTemplate.setVariable("$Wireframe_Attributes$", size);
                elementTemplate.setVariable("$Additional_Styles$", " border:1px solid #000000; ");
                elementTemplate.setVariable("$Element_Type$", element.getType().toString());
                elementTemplate.setVariable("$Element_Id$", element.getId());
                elementTemplate.setVariable("$Additional_Values$", " ");
                elementTemplate.setVariable("$Element_Content$", element.getCodeSample());
                elementTemplate.setVariable("$Closing_Element$", "</" + element.getType().toString() + ">");
                break;
            default:
                break;
        }


        return elementTemplate;
    }


    /**
     * Adds functions to the passed application script code, according to a frontend component model.
     *
     * @param applicationTemplate          the template of the application script
     * @param functionTemplateFile         a template representing a generic function
     * @param microserviceCallTemplateFile a template representing a generic microservice call
     * @param iwcResponseTemplateFile      a template representing a generic IWC response (with function
     *                                     call)
     * @param htmlElementTemplateFile      a template representing a generic HTML template
     * @param frontendComponent            a {@link FrontendComponent}
     */
    public static void createApplicationScript(Template applicationTemplate,
                                               String functionTemplateFile, String microserviceCallTemplateFile,
                                               String iwcResponseTemplateFile, String htmlElementTemplateFile,
                                               FrontendComponent frontendComponent) {

        // add trace to application script
        applicationTemplate.getTemplateEngine().addTrace(frontendComponent.getWidgetModelId(),
                "ApplicationScript", "ApplicationScript", applicationTemplate.getSegment());

        // first the endpoint
        applicationTemplate.setVariable("$Microservice_Endpoint$",
                frontendComponent.getMicroserviceAddress());

        // now to the functions
        for (Function function : frontendComponent.getFunctions().values()) {
            Template functionTemplate = applicationTemplate.createTemplate(
                    applicationTemplate.getId() + function.getModelId(), functionTemplateFile);

            applicationTemplate.getTemplateEngine().addTrace(function.getModelId(), "Function",
                    function.getName(), functionTemplate);
            // add function to application script
            applicationTemplate.appendVariable("$Functions$", functionTemplate);

            // start with (potential) IWC response creation
            for (IWCResponse response : function.getIwcResponses()) {

                Template iwcResponseTemplate = applicationTemplate.createTemplate(
                        applicationTemplate.getId() + response.getModelId(), iwcResponseTemplateFile);

                iwcResponseTemplate.setVariable("$Intent_Action$", response.getIntentAction());
                iwcResponseTemplate.setVariable("$Function_Name$", function.getName());

                // add IWC response to application script
                applicationTemplate.appendVariable("$IWC_Responses$", iwcResponseTemplate);
                applicationTemplate.getTemplateEngine().addTrace(response.getModelId(),
                        "IWC Response[" + response.getIntentAction() + "]", iwcResponseTemplate);
            }


            List<HtmlElement> updatedElements = new ArrayList<HtmlElement>();

            // element updates
            for (String elementId : function.getHtmlElementUpdates()) {
                HtmlElement element = frontendComponent.getHtmlElements().get(elementId);
                updatedElements.add(element);
            }


            // start creating the actual function
            functionTemplate.setVariable("$Function_Name$", function.getName());

            functionTemplate.appendVariable("$Function_Body$", applicationTemplate.createTemplate(
                    functionTemplate.getId() + ":startVarDeclaration", "//start variable declaration\n"));
            // function parameters
            if (!function.getIwcResponses().isEmpty()) {
                // all content names are equal (checked in model creation process), so just take the first
                functionTemplate.setVariable("$Function_Parameters$",
                        function.getIwcResponses().get(0).getContent());
            }
            // treat special case when function does not have any input parameter
            else if (function.getInputParameters().isEmpty()) {
                functionTemplate.setVariable("$Function_Parameters$", "");
            } else {
                String parameters = "";
                for (InputParameter parameter : function.getInputParameters()) {
                    parameters += parameter.getName() + ", ";
                }

                // remove last input parameter placeholder
                parameters = parameters.substring(0, Math.max(0, parameters.length() - 2));

                functionTemplate.setVariable("$Function_Parameters$", parameters);
            }
            // check for return parameter (if there is one, else just remove placeholder)
            if (!function.getReturnParameter().equals("")) {
                // add variable initialization
                Template returnParameter = applicationTemplate
                        .createTemplate(function.getModelId() + ":return:param", "   var $param$ = null;\n");

                returnParameter.setVariable("$param$", function.getReturnParameter());
                functionTemplate.appendVariable("$Function_Body$", returnParameter);
                functionTemplate.setVariable("$Function_Return_Parameter$",
                        " return " + function.getReturnParameter() + ";");

            } else {
                functionTemplate.setVariable("$Function_Return_Parameter$", "");
            }

            functionTemplate.appendVariable("$Function_Body$",
                    applicationTemplate.createTemplate(functionTemplate.getId() + ":endVarDeclaration",
                            "-{\n}-//end variable declaration\n-{\n}-"));


            // microservice calls
            for (MicroserviceCall microserviceCall : function.getMicroserviceCalls()) {
                Template microserviceCallFile = applicationTemplate.createTemplate(
                        functionTemplate.getId() + microserviceCall.getModelId(), microserviceCallTemplateFile);

                microserviceCallFile.setVariable("$Method_Type$",
                        microserviceCall.getMethodType().toString());

                microserviceCallFile.setVariable("$Method_Path$", microserviceCall.getPath());

                if (microserviceCall.isAuthorize()) {
                    microserviceCallFile.setVariable("$Authenticate$", "true");
                } else {
                    microserviceCallFile.setVariable("$Authenticate$", "false");
                }
                if (!microserviceCall.getContent().isEmpty()) {
                    // add variable initialization
                    Template contentVar = applicationTemplate.createTemplate(
                            microserviceCallFile.getId() + "contentVar", "   var $Content$ = null;-{\n}-");
                    contentVar.setVariable("$Content$", microserviceCall.getContent());

                    functionTemplate.appendVariable("$Function_Body$", contentVar);

                    microserviceCallFile.setVariable("$Content$", microserviceCall.getContent());

                    // TODO workaround
                    String contentType = microserviceCall.getContentType().toString();
                    if (contentType.equals("application/json"))
                        contentType = "text/plain";

                    microserviceCallFile.setVariable("$Content_Type$", contentType);
                } else {
                    // no content specified, just remove placeholder / insert empty entries
                    microserviceCallFile.setVariable("$Content$", "\"\"");
                    microserviceCallFile.setVariable("$Content_Type$", "");
                }

                microserviceCallFile.setVariable("$Method_Path$", microserviceCall.getPath());

                for (HtmlElement element : updatedElements) {
                    String updateElementTemplate =
                            "\n-{    //Also update the html element?\n    //}-$(\"#$Element_Id$\").html(-{\"Updated Element\"}-);";
                    // success callback
                    Template updatedElement = applicationTemplate.createTemplate(functionTemplate.getId()
                                    + ":" + microserviceCall.getModelId() + ":update:" + element.getModelId(),
                            updateElementTemplate);
                    updatedElement.setVariable("$Element_Id$", element.getId());
                    // error callback
                    Template updatedElementError = applicationTemplate.createTemplate(functionTemplate.getId()
                                    + ":" + microserviceCall.getModelId() + ":updateError:" + element.getModelId(),
                            updateElementTemplate);
                    updatedElementError.setVariable("$Element_Id$", element.getId());

                    microserviceCallFile.appendVariable("$HTML_Elements_Updates$", updatedElement);
                    microserviceCallFile.appendVariable("$HTML_Elements_Updates_Errors$",
                            updatedElementError);
                }

                microserviceCallFile.setVariableIfNotSet("$HTML_Elements_Updates$", "");
                microserviceCallFile.setVariableIfNotSet("$HTML_Elements_Updates_Errors$", "");

                functionTemplate.appendVariable("$Function_Body$", microserviceCallFile);
                applicationTemplate.getTemplateEngine().addTrace(microserviceCall.getModelId(),
                        "MicroserviceCall", microserviceCallFile);
            }

            // element creations
            for (String elementId : function.getHtmlElementCreations()) {
                HtmlElement element = frontendComponent.getHtmlElements().get(elementId);

                Template elementTemplate =
                        createHtmlElementTemplate(element, htmlElementTemplateFile, functionTemplate);

                Template createdElementTemplate = applicationTemplate.createTemplate(
                        functionTemplate.getId() + ":create:" + element.getModelId(),
                        "   $( \".container\" ).append(\"$Html$\");\n");

                createdElementTemplate.appendVariable("$Html$", elementTemplate);
                functionTemplate.appendVariable("$Function_Body$", createdElementTemplate);

                applicationTemplate.getTemplateEngine().addTrace(element.getModelId(), "HTML Element",
                        element.getId(), elementTemplate);
            }

            // element updates
            for (String elementId : function.getHtmlElementUpdates()) {
                HtmlElement element = frontendComponent.getHtmlElements().get(elementId);

                Template updatedElement = applicationTemplate.createTemplate(
                        functionTemplate.getId() + ":update:" + element.getModelId(),
                        "\n-{  }-$(\"#$Element_Id$\").html(-{\"Updated Element\"}-);");
                updatedElement.setVariable("$Element_Id$", element.getId());
                functionTemplate.appendVariable("$Function_Body$", updatedElement);
            }

            // iwc calls
            for (IWCCall call : function.getIwcCalls()) {

                Template iwc = applicationTemplate.createTemplate(
                        functionTemplate.getId() + ":iwc:" + call.getModelId(),
                        "\n  var $Content$ = \"initialized\";-{\n"
                                + "  }-client.sendIntent(\"$Intent_Action$\",$Content$,-{true}-);\n");

                iwc.setVariable("$Content$", call.getContent());

                iwc.setVariable("$Intent_Action$", call.getIntentAction());
                iwc.setVariable("$Content$", call.getContent());

                functionTemplate.appendVariable("$Function_Body$", iwc);
                applicationTemplate.getTemplateEngine().addTrace(call.getModelId(), "IWC Call",
                        call.getIntentAction(), iwc);

            }


        }


        applicationTemplate.setVariableIfNotSet("$Functions$", "");
        applicationTemplate.setVariableIfNotSet("$Events$", "");
        applicationTemplate.setVariableIfNotSet("$IWC_Responses$", "");

    }


    /**
     * Adds the (possible) Yjs collaboration code to the application script.
     * <p>
     * TODO: Currently, only textareas are supported.
     *
     * @param applicationScriptTemplate the template of the application script code
     * @param templateEngine            the template engine used during the code generation
     * @param yjsInitTemplateFile       a template for initializing the Yjs connector etc
     * @param frontendComponent         a {@link FrontendComponent}
     */
    public static void addYjsCollaboration(Template applicationScriptTemplate,
                                           TemplateEngine templateEngine, String yjsInitTemplateFile,
                                           FrontendComponent frontendComponent) {

        boolean foundCollaborativeElement = false; // helper so that the code only needs to run once

        Template yjsInitTemplate = templateEngine
                .createTemplate(applicationScriptTemplate.getId() + ":yjsInit", yjsInitTemplateFile);
        String variableInit = "$Variable_Init$";
        String shareElement = "$Share_Element$";

        for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
            if (element.isCollaborativeElement()) {
                if (!foundCollaborativeElement) {
                    applicationScriptTemplate.appendVariable("$Yjs_Code$", yjsInitTemplate);
                    foundCollaborativeElement = true;
                }

                variableInit = variableInit.replace("$Variable_Init$",
                        "    " + element.getId() + ":\'Text\'" + ",\n$Variable_Init$");
                shareElement = shareElement.replace("$Share_Element$", "  y.share." + element.getId()
                        + ".bind(document.getElementById('" + element.getId() + "'))" + "\n$Share_Element$");

            }
        }

        // remove last placeholder
        variableInit = variableInit.replace(",\n$Variable_Init$", "");
        variableInit = variableInit.replace("$Variable_Init$", "");
        shareElement = shareElement.replace("\n$Share_Element$", "");
        shareElement = shareElement.replace("$Share_Element$", "");

        // finally set the variables in template
        yjsInitTemplate.setVariable("$Variable_Init$", variableInit);
        yjsInitTemplate.setVariable("$Share_Element$", shareElement);

        applicationScriptTemplate.setVariableIfNotSet("$Yjs_Code$", "");

    }


    /**
     * Adds events to the passed application script for all HTML elements.
     *
     * @param applicationScriptTemplate the template of the application script
     * @param templateEngine            a template engine to use
     * @param eventTemplateFile         a template for an event
     * @param frontendComponent         a {@link FrontendComponent}
     */
    public static void addEventsToApplicationScript(Template applicationScriptTemplate,
                                                    TemplateEngine templateEngine, String eventTemplateFile,
                                                    FrontendComponent frontendComponent) {

        for (HtmlElement element : frontendComponent.getHtmlElements().values()) {
            for (Event event : element.getEvents()) {
                Template eventTemplate = templateEngine
                        .createTemplate(element.getModelId() + ":" + event.getModelId(), eventTemplateFile);
                eventTemplate.setVariable("$Html_Element_Id$", element.getId());

                applicationScriptTemplate.appendVariable("$Events$", eventTemplate);

                eventTemplate.getTemplateEngine().addTrace(event.getModelId(), "Event",
                        eventTemplate.getSegment());

                eventTemplate.setVariable("$Event_Type$", event.getEventCause().toString());

                Function function = frontendComponent.getFunctions().get(event.getCalledFunctionId());

                eventTemplate.setVariable("$Function_Name$", function.getName());

                String arguments = "$Function_Parameter$";
                boolean firstParameter = true;

                for (InputParameter parameter : function.getInputParameters()) {

                    Template inputParameter = templateEngine.createTemplate(
                            eventTemplate.getId() + ":input:param:" + parameter.getModelId(),
                            "    var $Parameter_Name$ = null;");

                    inputParameter.setVariable("$Parameter_Name$", parameter.getName());

                    if (firstParameter) {
                        firstParameter = false;
                    } else {
                        eventTemplate.insertBreakLine(parameter.getModelId(), "$Parameter_Init$");
                    }

                    eventTemplate.appendVariable("$Parameter_Init$", inputParameter);
                    arguments = arguments.replace("$Function_Parameter$",
                            parameter.getName() + ", $Function_Parameter$");
                }

                if (!firstParameter) {
                    eventTemplate.insertBreakLine("additional", "$Parameter_Init$");
                }


                // remove last parameter placeholder
                arguments = arguments.replace(", $Function_Parameter$", "");
                // special case: no parameter
                arguments = arguments.replace("$Function_Parameter$", "");
                eventTemplate.setVariable("$Function_Parameter$", arguments);

                eventTemplate.setVariableIfNotSet("$Parameter_Init$", "");
            }
        }
    }
}