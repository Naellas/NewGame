package com.alderfall.game.editor;

import com.alderfall.game.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Independent desktop authoring shell. No adventure or save slot is loaded. */
public final class MapEditorWindow extends JFrame {
    enum DragMode { COPIES, OFFSET, SINGLE }
    enum Tool { SELECT, PAINT, FILL, RECTANGLE, LINE, PLACE, PATCH, ERASE, SPAWN, LANDMARK, STAMP }
    final Path root;
    final GamePanel renderer;
    final AssetStore assets;
    MapDocument document;
    EditorHistory history;
    MapDocument clipboard;
    Path file;
    private boolean unsavedDocument;
    final EditorCanvas canvas;
    final JLabel status=new JLabel("Ready"),selectionInfo=new JLabel("Select an object or drag a region");
    final JComboBox<Tool> tool=new JComboBox<>(Tool.values());
    final JSpinner brush=new JSpinner(new SpinnerNumberModel(1,1,15,1));
    final JSpinner offsetX=new JSpinner(new SpinnerNumberModel(0,-48,48,1)),offsetY=new JSpinner(new SpinnerNumberModel(0,-48,48,1));
    final JSpinner density=new JSpinner(new SpinnerNumberModel(70,0,100,5)),variation=new JSpinner(new SpinnerNumberModel(20,0,50,5));
    final JCheckBox protectGround=new JCheckBox("Patches avoid roads / objects",true);
    final JSpinner spacing=new JSpinner(new SpinnerNumberModel(1,1,16,1));
    final JComboBox<DragMode> dragMode=new JComboBox<>(DragMode.values());
    final JComboBox<String> subcategory=new JComboBox<>(new String[]{"All subcategories"});
    final JComboBox<String> sort=new JComboBox<>(new String[]{"Name A-Z","Name Z-A","Newest modified","Oldest modified","Category"});
    final JLabel modifiedInfo=new JLabel("Modified: unknown");
    final Map<String,EditorAssetBrowser.Info> assetInfo=new HashMap<>();
    private boolean updatingFilters;
    final JSpinner propSize=new JSpinner(new SpinnerNumberModel(48,1,512,4));
    final JComboBox<CityBuilding.Facing> facing=new JComboBox<>(CityBuilding.Facing.values());
    final JCheckBox grid=new JCheckBox("Grid",true),collision=new JCheckBox("Collision"),
        terrainLayer=new JCheckBox("Terrain",true),buildingLayer=new JCheckBox("Buildings",true),
        propLayer=new JCheckBox("Props",true),markerLayer=new JCheckBox("Markers",true),stampTerrain=new JCheckBox("Stamp terrain",true);
    final JTextField markerText=new JTextField("Location name"),search=new JTextField();
    final JList<EditorPalette.Entry> palette=new JList<>(new DefaultListModel<>());
    final List<EditorPalette.Entry> entries;
    final JComboBox<String> category;
    final JLabel preview=new JLabel("",SwingConstants.CENTER);
    final JLabel libraryCount=new JLabel();
    final Map<String,String> assetSources=new HashMap<>();
    EditorPalette.Entry entry;
    Rectangle selection;
    int selectedProp=-1,selectedBuilding=-1;
    final Set<Integer> selectedProps=new LinkedHashSet<>(),selectedBuildings=new LinkedHashSet<>();
    final JCheckBox liveProperties=new JCheckBox("Apply property changes immediately",true);
    boolean loadingInspector;
    private int inspectorOffsetX,inspectorOffsetY;
    void setOffsets(int x,int y){boolean previous=loadingInspector;loadingInspector=true;try{offsetX.setValue(x);offsetY.setValue(y);}finally{loadingInspector=previous;}}
    void showProp(WorldProp p){loadingInspector=true;try{propSize.setValue(p.size());setOffsets(p.offsetX(),p.offsetY());}finally{loadingInspector=false;}}
    Set<Integer> propSelection(){Set<Integer> result=new LinkedHashSet<>(selectedProps);if(selectedProp>=0)result.add(selectedProp);if(selection!=null)for(int i=0;i<document.props.size();i++){WorldProp p=document.props.get(i);if(selection.contains(p.x(),p.y()))result.add(i);}return result;}
    Set<Integer> buildingSelection(){Set<Integer> result=new LinkedHashSet<>(selectedBuildings);if(selectedBuilding>=0)result.add(selectedBuilding);if(selection!=null)for(int i=0;i<document.buildings.size();i++)if(selection.contains(MapDocument.rect(document.buildings.get(i))))result.add(i);return result;}

    private final List<GamePanel> playtests=new ArrayList<>();

    public static void launch(Path root,Path initial) {
        SwingUtilities.invokeLater(() -> {try{openOnEventThread(root,initial);}catch(RuntimeException ex){JOptionPane.showMessageDialog(null,ex.getMessage(),"Could not open workshop",JOptionPane.ERROR_MESSAGE);}});
    }
    public static void openOnEventThread(Path root,Path initial) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open editor windows on the Swing event thread.");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { }
        LoadingScreen.run(null,"Opening Location Workshop",progress -> prepareEditor(root,initial,progress),prepared -> new MapEditorWindow(root,prepared).setVisible(true));
    }
    private record PreparedEditor(GamePanel.Prepared game,List<EditorPalette.Entry> entries,Map<String,String> sources,
                                  Map<String,EditorAssetBrowser.Info> info,MapDocument document,Path initial){}
    private static PreparedEditor prepareEditor(Path root,Path initial,java.util.function.Consumer<String> progress){
        GamePanel.Prepared game=GamePanel.prepare(root,progress);
        progress.accept("Organizing the asset library and categories...");
        var entries=EditorPalette.entries(game.assets());Map<String,String> sources=new HashMap<>();Map<String,EditorAssetBrowser.Info> info=new HashMap<>();
        progress.accept("Reading asset paths and modification dates...");
        for(var e:entries){sources.computeIfAbsent(e.asset(),game.assets()::assetRelativePath);info.computeIfAbsent(e.asset(),id -> EditorAssetBrowser.inspect(root.resolve("assets"),sources.get(id)));}
        progress.accept(initial==null?"Generating the starter map...":"Reading the map document...");
        MapDocument doc;
        try{doc=initial==null?EditorLayouts.create("Untitled city","city",64,48,42,true):MapDocumentIO.read(initial);}catch(IOException ex){throw new java.io.UncheckedIOException(ex);}
        progress.accept("Preparing asset previews...");
        entries.stream().sorted(Comparator.comparing(EditorPalette.Entry::label,String.CASE_INSENSITIVE_ORDER)).limit(8).forEach(e -> game.assets().placementSpriteFit(e.asset(),46,46));
        return new PreparedEditor(game,entries,sources,info,doc,initial);
    }
    public MapEditorWindow(Path root) {this(root,prepareEditor(root,null,message -> {}));}
    private MapEditorWindow(Path root,PreparedEditor prepared) {
        super("Alderfall — Location Workshop");
        this.root=root;renderer=new GamePanel(root,false,prepared.game());assets=prepared.game().assets();
        document=prepared.document();history=new EditorHistory(document);file=prepared.initial();
        renderer.editorState().currentMapId="editor_workshop";
        entries=prepared.entries();assetSources.putAll(prepared.sources());assetInfo.putAll(prepared.info());
        TreeSet<String> categories=new TreeSet<>(); entries.forEach(e -> categories.add(EditorAssetBrowser.category(e)));
        categories.add("All assets"); category=new JComboBox<>(categories.toArray(String[]::new)); category.setSelectedItem("All assets");
        canvas=new EditorCanvas(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { if (confirmDiscard()) {
            for(GamePanel panel:List.copyOf(playtests)){panel.shutdown();Window game=SwingUtilities.getWindowAncestor(panel);if(game!=null)game.dispose();}
            renderer.shutdown();dispose();
        } } });
        setJMenuBar(menu());
        JPanel top=new JPanel(new BorderLayout());
        JLabel title=new JLabel("  ALDERFALL  /  LOCATION WORKSHOP"); title.setFont(title.getFont().deriveFont(Font.BOLD,18f)); title.setBorder(BorderFactory.createEmptyBorder(12,4,10,4)); top.add(title,BorderLayout.NORTH);
        JToolBar bar=new JToolBar(); bar.setFloatable(false);
        addButton(bar,"New",this::newDocument); addButton(bar,"Open",this::chooseOpen); addButton(bar,"Save",() -> save(false));
        bar.addSeparator(); addButton(bar,"Undo",this::undo); addButton(bar,"Redo",this::redo); bar.addSeparator();
        addButton(bar,"Playtest",this::playtest); addButton(bar,"Validate",this::validateMap); addButton(bar,"Fit",canvas::fit);
        tool.setMaximumSize(new Dimension(160,30));brush.setMaximumSize(new Dimension(70,30));
        bar.addSeparator(); bar.add(tool); bar.add(new JLabel(" Brush ")); bar.add(brush);bar.add(Box.createHorizontalGlue());
        top.add(bar,BorderLayout.SOUTH); add(top,BorderLayout.NORTH);
        JSplitPane workspace=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,library(),canvas);
        workspace.setDividerLocation(285); workspace.setResizeWeight(0);
        JSplitPane full=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,workspace,inspector()); full.setResizeWeight(1); full.setDividerLocation(1050);
        add(full,BorderLayout.CENTER); status.setBorder(BorderFactory.createEmptyBorder(7,12,7,12)); add(status,BorderLayout.SOUTH);
        setSize(1380,900); setMinimumSize(new Dimension(1000,640)); setLocationRelativeTo(null);
        tool.addActionListener(e -> canvas.repaint());
        for (JCheckBox box:List.of(grid,collision,terrainLayer,buildingLayer,propLayer,markerLayer)) box.addActionListener(e -> canvas.repaint());
        refreshPalette(); sync();
        SwingUtilities.invokeLater(canvas::fit);
    }
    private JMenuBar menu() {
        JMenuBar bar=new JMenuBar(); JMenu fileMenu=new JMenu("File"),edit=new JMenu("Edit"),prefab=new JMenu("Prefabs"),help=new JMenu("Help");
        item(fileMenu,"New location","control N",this::newDocument); item(fileMenu,"Open map…","control O",this::chooseOpen);
        item(fileMenu,"Save","control S",() -> save(false)); item(fileMenu,"Save as…","control shift S",() -> save(true));
        item(fileMenu,"Playtest in game","F5",this::playtest); item(fileMenu,"Validate map",null,this::validateMap);
        item(edit,"Undo","control Z",this::undo); item(edit,"Redo","control Y",this::redo);
        item(edit,"Copy region / object","control C",this::copy); item(edit,"Stamp copied prefab","control V",() -> { if (clipboard!=null) tool.setSelectedItem(Tool.STAMP); });
        item(edit,"Delete selected objects","DELETE",() -> {if(!(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof javax.swing.text.JTextComponent))deleteSelection();});
        item(edit,"Select whole map","control A",() -> { clearSelection(); selection=new Rectangle(0,0,document.width(),document.height()); canvas.repaint(); });
        item(prefab,"Save selection as prefab…",null,this::savePrefab); item(prefab,"Load prefab…",null,this::loadPrefab);
        item(prefab,"Rotate stamp clockwise","control R",this::rotateStamp);
        item(help,"Controls and scope",null,() -> JOptionPane.showMessageDialog(this,
            "Left click/drag: active tool. Right/middle drag: pan. Wheel: zoom at cursor.\n"
            +"Select: drag empty terrain for a rectangle, or drag an object to move it.\n"
            +"Paint uses brush size; Rectangle/Line drag terrain; Fill replaces connected terrain.\n"
            +"Copy a selection, then Stamp repeatedly. Ctrl+R rotates the stamp.\n"
            +"Erase removes topmost objects/markers; terrain is changed with painting tools.\n"
            +"Buildings are functional village plans; all other library entries are visual scenery.\n"
            +"Drag mode: COPIES paints copies; OFFSET moves one prop freely; SINGLE places once. Ctrl-select assets to drag a row from the library.\n"
            +"PATCH paints ground cover on four quarter-tile anchors; Ctrl-select plants to mix.\n"
            +"Offset X/Y and quarter-anchor buttons shift prop artwork. Alt+arrows nudge a selected prop.\n"
            +"Drag inside a selected rectangle to move its objects together.\n"
            +"Animals/NPC sprites have no AI. Animation strips use their first pose; atlases remain sheets.\n"
            +"Collision overlay and validation use game movement rules.\n"
            +"Landmarks are names, not scripted quests or encounters.\n"
            +"Building interiors are generated by the game during playtest.\n"
            +"Prefab rotation changes layout and building facing; prop artwork retains orientation."));
        bar.add(fileMenu);bar.add(edit);bar.add(prefab);bar.add(help);return bar;
    }
    private void item(JMenu menu,String label,String shortcut,Runnable action) {
        JMenuItem item=new JMenuItem(label); if(shortcut!=null)item.setAccelerator(KeyStroke.getKeyStroke(shortcut)); item.addActionListener(e -> action.run()); menu.add(item);
    }
    private JComponent library() {
        JPanel panel=new JPanel(new BorderLayout(6,6)); panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,8));
        JPanel filters=new JPanel(new GridLayout(0,1,4,5)); filters.add(new JLabel("ASSET LIBRARY")); filters.add(category); filters.add(subcategory); filters.add(sort);search.setToolTipText("Search asset label or logical ID"); filters.add(search); panel.add(filters,BorderLayout.NORTH);
        // Both dimensions must be fixed: otherwise Swing measures every entry by decoding its thumbnail.
        palette.setFixedCellHeight(76); palette.setFixedCellWidth(245); palette.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        palette.setDragEnabled(true);
        palette.setTransferHandler(new TransferHandler(){
            public int getSourceActions(JComponent c){return COPY;}
            protected java.awt.datatransfer.Transferable createTransferable(JComponent c){var selected=palette.getSelectedValuesList();return selected.isEmpty()?null:new java.awt.datatransfer.StringSelection(String.join("\n",selected.stream().map(EditorPalette.Entry::asset).toList()));}
        });
        palette.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focused) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focused);
                var e=(EditorPalette.Entry)value;
                label.setText("<html>"+html(e.label())+"<br><small>"+html(e.category())+"<br>Modified: "+assetInfo.get(e.asset()).date()+"</small></html>");
                label.setIcon(new ImageIcon(assets.placementSpriteFit(e.asset(),46,46))); label.setIconTextGap(8);
                label.setToolTipText(e.asset()+" — "+assetSources.getOrDefault(e.asset(),""));return label;
            }
        });
        palette.addListSelectionListener(e -> { if(e.getValueIsAdjusting())return; entry=palette.getSelectedValue(); if(entry!=null) {
            clearSelection();propSize.setValue(entry.size()); preview.setIcon(new ImageIcon(assets.placementSpriteFit(entry.asset(),180,150))); preview.setToolTipText(entry.asset()+" ? "+assetSources.get(entry.asset())); modifiedInfo.setText("Modified: "+assetInfo.get(entry.asset()).date()); modifiedInfo.setToolTipText("Source file modification time (local timezone): "+assetSources.get(entry.asset()));
            if(entry.terrain()) { if(!List.of(Tool.PAINT,Tool.FILL,Tool.LINE,Tool.RECTANGLE).contains(tool.getSelectedItem()))tool.setSelectedItem(Tool.PAINT); }
            else if(tool.getSelectedItem()!=Tool.PATCH)tool.setSelectedItem(Tool.PLACE);
        }});
        panel.add(new JScrollPane(palette),BorderLayout.CENTER);
        category.addActionListener(e -> {updatingFilters=true;subcategory.removeAllItems();subcategory.addItem("All subcategories");
            entries.stream().filter(a -> "All assets".equals(category.getSelectedItem())||EditorAssetBrowser.category(a).equals(category.getSelectedItem())).map(EditorAssetBrowser::subcategory).distinct().sorted().forEach(subcategory::addItem);
            updatingFilters=false;refreshPalette();});
        subcategory.addActionListener(e -> {if(!updatingFilters)refreshPalette();});sort.addActionListener(e -> refreshPalette()); search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){refreshPalette();} public void removeUpdate(DocumentEvent e){refreshPalette();} public void changedUpdate(DocumentEvent e){refreshPalette();}
        });
        panel.add(libraryCount,BorderLayout.SOUTH);return panel;
    }
    private JComponent inspector() {
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(12,10,10,12));p.setPreferredSize(new Dimension(265,600));
        p.add(new JLabel("PLACEMENT / SELECTION")); p.add(preview);p.add(modifiedInfo);p.add(selectionInfo);
        p.add(new JLabel("Prop size (at reference 48px tile)"));p.add(propSize);p.add(new JLabel("Building facing"));p.add(facing);
        p.add(new JLabel("Drag behavior (Place / Select)"));p.add(dragMode);dragMode.setToolTipText("COPIES: paint copies. OFFSET: drag one prop freely. SINGLE: click placement / grid movement.");p.add(new JLabel("Drag spacing (tiles)"));p.add(spacing);
        p.add(new JLabel("Visual offset X / Y (48 = one tile)"));
        JPanel offsets=new JPanel(new GridLayout(1,2,4,0));offsets.add(offsetX);offsets.add(offsetY);p.add(offsets);
        JPanel anchors=new JPanel(new GridLayout(2,2,3,3));
        for(int q=0;q<4;q++){final int quadrant=q;JButton anchor=new JButton(new String[]{"NW","NE","SW","SE"}[q]);anchor.setToolTipText("Place the sprite root at this quarter-tile anchor");anchor.addActionListener(e -> {offsetX.setValue(quadrant%2==0?-12:12);offsetY.setValue(quadrant/2==0?-36:-12);});anchors.add(anchor);}p.add(anchors);
        offsetX.addChangeListener(e -> canvas.repaint());offsetY.addChangeListener(e -> canvas.repaint());
        JButton resetOffset=new JButton("Reset offset");resetOffset.addActionListener(e -> {offsetX.setValue(0);offsetY.setValue(0);});p.add(resetOffset);
        p.add(new JLabel("Alt + arrows: nudge selected prop"));
        p.add(new JLabel("PATCH: density % / variation %"));
        JPanel patch=new JPanel(new GridLayout(1,2,4,0));patch.add(density);patch.add(variation);p.add(patch);p.add(protectGround);
        p.add(new JLabel("Ctrl-select plants to mix in a patch"));
        JButton apply=new JButton("Apply size / offset / facing");apply.addActionListener(e -> applyInspector());p.add(apply);p.add(liveProperties);
        propSize.addChangeListener(e -> propertyChanged("size"));offsetX.addChangeListener(e -> propertyChanged("x"));offsetY.addChangeListener(e -> propertyChanged("y"));
        facing.addActionListener(e -> propertyChanged("facing"));
        JButton delete=new JButton("Delete selected objects");delete.addActionListener(e -> deleteSelection());p.add(delete);
        p.add(Box.createVerticalStrut(16));p.add(new JLabel("Landmark name"));p.add(markerText);
        p.add(Box.createVerticalStrut(14));p.add(new JLabel("VIEW LAYERS"));
        for(JCheckBox box:List.of(terrainLayer,buildingLayer,propLayer,markerLayer,grid,collision,stampTerrain))p.add(box);
        p.add(Box.createVerticalStrut(14));
        for (Object[] command:new Object[][]{{"Copy selection",(Runnable)this::copy},{"Save prefab…",(Runnable)this::savePrefab},{"Load prefab…",(Runnable)this::loadPrefab},{"Rotate stamp",(Runnable)this::rotateStamp}}) {
            JButton b=new JButton((String)command[0]);b.addActionListener(e -> ((Runnable)command[1]).run());p.add(b);
        }
        p.add(Box.createVerticalStrut(12));p.add(new JLabel("OVERVIEW (click to navigate)"));p.add(canvas.minimap);
        p.add(Box.createVerticalGlue());
        for(Component c:p.getComponents()) if(c instanceof JComponent jc) { jc.setAlignmentX(0); if(!(c==canvas.minimap)) jc.setMaximumSize(new Dimension(260,c==preview?155:c.getPreferredSize().height+4)); }
        return new JScrollPane(p);
    }
    private static String html(String s) { return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    private static void addButton(JToolBar bar,String label,Runnable action) { JButton b=new JButton(label);b.addActionListener(e -> action.run());bar.add(b); }
    private void refreshPalette() {
        String query=search.getText().toLowerCase(Locale.ROOT),cat=(String)category.getSelectedItem(),sub=(String)subcategory.getSelectedItem();
        EditorPalette.Entry selected=palette.getSelectedValue();
        var matches=entries.stream().filter(e -> ("All assets".equals(cat)||EditorAssetBrowser.category(e).equals(cat))
            &&("All subcategories".equals(sub)||EditorAssetBrowser.subcategory(e).equals(sub))
            &&(e.label()+" "+e.asset()+" "+e.category()+" "+assetSources.getOrDefault(e.asset(),"")).toLowerCase(Locale.ROOT).contains(query))
            .sorted(EditorAssetBrowser.comparator((String)sort.getSelectedItem(),assetInfo)).toList();
        DefaultListModel<EditorPalette.Entry> model=new DefaultListModel<>();model.addAll(matches);palette.setModel(model);
        libraryCount.setText(model.size()+" / "+entries.size()+" entries");
        if(selected!=null&&matches.contains(selected))palette.setSelectedValue(selected,true);else if(!model.isEmpty())palette.setSelectedIndex(0);
    }
    void nudge(int dx,int dy){
        var selected=propSelection();if(selected.isEmpty())return;
        for(int i:selected){WorldProp next=EditorPropPosition.shift(document.props.get(i),dx,dy);if(!document.contains(next.x(),next.y()))return;}
        history.begin(document);for(int i:selected)document.props.set(i,EditorPropPosition.shift(document.props.get(i),dx,dy));
        WorldProp last=document.props.get(selected.iterator().next());setOffsets(last.offsetX(),last.offsetY());changed();
    }
    void sync() {
        document.install(renderer.editorState().world,"editor_workshop");
        setTitle((unsavedDocument||history.dirty(document)?"* ":"")+document.label+" — Alderfall Location Workshop");
        canvas.repaint(); canvas.minimap.repaint();
    }
    void changed() { history.commit(document); sync(); }
    void clearSelection() { selectedProps.clear();selectedBuildings.clear();selectedProp=-1;selectedBuilding=-1;selection=null;selectionInfo.setText("Select an object or drag a region"); }
    void undo() { document=history.undo(document);clearSelection();sync(); }
    void redo() { document=history.redo(document);clearSelection();sync(); }
    void info(String s) {status.setText(s);}
    void error(Exception ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Location Workshop",JOptionPane.ERROR_MESSAGE); }
    private void newDocument() {
        JTextField name=new JTextField("New location"),seed=new JTextField("42");
        JComboBox<String> kind=new JComboBox<>(new String[]{"city","village","dungeon","interior"});
        JSpinner w=new JSpinner(new SpinnerNumberModel(64,10,160,2)),h=new JSpinner(new SpinnerNumberModel(48,8,160,2));JCheckBox generate=new JCheckBox("Generate starting layout",true);
        JPanel form=new JPanel(new GridLayout(0,2,8,8));for(Object[] row:new Object[][]{{"Name",name},{"Kind",kind},{"Width",w},{"Height",h},{"Seed",seed},{"",generate}}){form.add(new JLabel((String)row[0]));form.add((Component)row[1]);}
        if(JOptionPane.showConfirmDialog(this,form,"New location",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try {
            MapDocument next=EditorLayouts.create(name.getText(),(String)kind.getSelectedItem(),(int)w.getValue(),(int)h.getValue(),Long.parseLong(seed.getText().trim()),generate.isSelected());
            if(!confirmDiscard())return; document=next;history=new EditorHistory(next);file=null;unsavedDocument=true;clearSelection();sync();canvas.fit();
        } catch(Exception ex){error(ex);}
    }
    private boolean confirmDiscard() {
        if(!unsavedDocument&&!history.dirty(document))return true;
        int answer=JOptionPane.showConfirmDialog(this,"Save changes to "+document.label+"?","Unsaved changes",JOptionPane.YES_NO_CANCEL_OPTION);
        return answer==JOptionPane.NO_OPTION || answer==JOptionPane.YES_OPTION&&save(false);
    }
    private Path choose(boolean save,boolean prefab) {
        Path folder=root.resolve("exports/map-editor").resolve(prefab?"prefabs":"maps");
        try{Files.createDirectories(folder);}catch(IOException ex){error(ex);return null;}
        JFileChooser chooser=new JFileChooser(folder.toFile());chooser.setFileFilter(new FileNameExtensionFilter("Alderfall maps / prefabs (*.aldermap, *.txt)","aldermap","txt"));
        if(save)chooser.setSelectedFile(folder.resolve(document.label.replaceAll("[^A-Za-z0-9_-]+","_")+(prefab?"_prefab":"")+".aldermap").toFile());
        if((save?chooser.showSaveDialog(this):chooser.showOpenDialog(this))!=JFileChooser.APPROVE_OPTION)return null;
        Path selected=chooser.getSelectedFile().toPath(); if(save&&!selected.getFileName().toString().contains("."))selected=selected.resolveSibling(selected.getFileName()+".aldermap");
        if(save&&Files.exists(selected)&&JOptionPane.showConfirmDialog(this,"Replace "+selected.getFileName()+"?","Overwrite file",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return null;
        return selected;
    }
    boolean save(boolean as) {
        Path target=as||file==null?choose(true,false):file;if(target==null)return false;
        try {MapDocumentIO.write(target,document);file=target;unsavedDocument=false;history.saved(document);sync();info("Saved "+target);return true;}catch(Exception ex){error(ex);return false;}
    }
    private void chooseOpen() {Path target=choose(false,false);if(target!=null)open(target);}
    void open(Path path) {
        try{MapDocument next=MapDocumentIO.read(path);if(!confirmDiscard())return;document=next;history=new EditorHistory(next);file=path;unsavedDocument=false;clearSelection();sync();canvas.fit();info("Opened "+path);}catch(Exception ex){error(ex);}
    }
    void copy() {
        clipboard=null;
        try {
            Rectangle r=selection;
            if(r==null&&selectedBuilding>=0)r=MapDocument.rect(document.buildings.get(selectedBuilding));
            if(r==null&&selectedProp>=0){WorldProp p=document.props.get(selectedProp);r=new Rectangle(p.x(),p.y(),1,1);}
            if(r==null){info("Select a region or object first.");return;}
            clipboard=document.extract(r);info("Copied "+r.width+"×"+r.height+" prefab. Ctrl+V to stamp; Ctrl+R to rotate.");
        }catch(Exception ex){error(ex);}
    }
    private void savePrefab(){copy();if(clipboard==null)return;Path target=choose(true,true);if(target!=null)try{MapDocumentIO.write(target,clipboard);info("Saved prefab "+target);}catch(Exception ex){error(ex);}}
    private void loadPrefab(){Path target=choose(false,true);if(target!=null)try{clipboard=MapDocumentIO.read(target);tool.setSelectedItem(Tool.STAMP);info("Loaded stamp "+clipboard.width()+"×"+clipboard.height());}catch(Exception ex){error(ex);}}
    private void rotateStamp(){if(clipboard!=null){clipboard=clipboard.rotate();canvas.repaint();info("Rotated stamp: "+clipboard.width()+"×"+clipboard.height());}}
    void deleteSelection(){
        Set<Integer> props=propSelection(),buildings=buildingSelection();history.begin(document);
        props.stream().sorted(Comparator.reverseOrder()).forEach(i -> document.props.remove((int)i));
        buildings.stream().sorted(Comparator.reverseOrder()).forEach(i -> document.buildings.remove((int)i));
        if(selection!=null)document.landmarks.keySet().removeIf(p -> selection.contains(p.x(),p.y()));
        clearSelection();changed();
    }
    private void propertyChanged(String property){
        if(!loadingInspector&&liveProperties.isSelected())applyProperty(property);
        if(loadingInspector||liveProperties.isSelected()){inspectorOffsetX=(int)offsetX.getValue();inspectorOffsetY=(int)offsetY.getValue();}
    }
    private void applyInspector(){applyProperty("all");inspectorOffsetX=(int)offsetX.getValue();inspectorOffsetY=(int)offsetY.getValue();}
    private void applyProperty(String property){
        Set<Integer> props=propSelection(),buildings=buildingSelection();if(props.isEmpty()&&buildings.isEmpty())return;
        MapDocument before=document.copy();history.begin(document);
        try{
            for(int i:props){WorldProp p=document.props.get(i);boolean all=property.equals("all");
                if(props.size()>1&&(all||property.equals("x")||property.equals("y"))){
                    WorldProp moved=EditorPropPosition.shift(p,all||property.equals("x")?(int)offsetX.getValue()-inspectorOffsetX:0,all||property.equals("y")?(int)offsetY.getValue()-inspectorOffsetY:0);
                    if(!document.contains(moved.x(),moved.y()))throw new IllegalArgumentException("Selection must remain inside the map.");
                    document.props.set(i,all?new WorldProp(moved.x(),moved.y(),moved.asset(),(int)propSize.getValue(),moved.visualSlot(),moved.offsetX(),moved.offsetY()):moved);continue;
                }
                document.props.set(i,new WorldProp(p.x(),p.y(),p.asset(),all||property.equals("size")?(int)propSize.getValue():p.size(),p.visualSlot(),
                    all||property.equals("x")?(int)offsetX.getValue():p.offsetX(),all||property.equals("y")?(int)offsetY.getValue():p.offsetY()));}
            if(property.equals("all")||property.equals("facing"))for(int i:buildings)document.faceBuilding(i,(CityBuilding.Facing)facing.getSelectedItem());
        }catch(IllegalArgumentException ex){document=before;error(ex);}
        changed();
    }
    private void validateMap(){
        sync(); var world=renderer.editorState().world; String id="editor_workshop"; List<String> issues=new ArrayList<>();
        if(!world.isPassable(id,document.spawnX,document.spawnY))issues.add("Spawn is blocked.");
        for(WorldProp p:document.props)if(!assets.hasSprite(p.asset()))issues.add("Missing asset: "+p.asset());
        int unreachable=0;
        boolean[][] seen=new boolean[document.height()][document.width()];ArrayDeque<TilePoint> queue=new ArrayDeque<>();
        if(world.isPassable(id,document.spawnX,document.spawnY)){queue.add(new TilePoint(document.spawnX,document.spawnY));seen[document.spawnY][document.spawnX]=true;}
        while(!queue.isEmpty()){TilePoint p=queue.remove();for(int[] a:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=p.x()+a[0],y=p.y()+a[1];if(document.contains(x,y)&&!seen[y][x]&&world.isPassable(id,x,y)){seen[y][x]=true;queue.add(new TilePoint(x,y));}}}
        for(int y=0;y<document.height();y++)for(int x=0;x<document.width();x++)if(!seen[y][x]&&world.isPassable(id,x,y))unreachable++;
        if(unreachable>0)issues.add(unreachable+" walkable tiles are unreachable from spawn (cardinal movement).");
        for(CityBuilding b:document.buildings){boolean access=false;for(TilePoint p:world.cityBuildingDoorTiles(b))if(document.contains(p.x(),p.y())&&seen[p.y()][p.x()])access=true;if(!access)issues.add("Check entrance access: "+VillageManager.buildingLabel(b.style())+" at "+b.x1()+","+b.y1());}
        JTextArea report=new JTextArea(issues.isEmpty()?"No missing assets, blocked spawn, or disconnected walkable regions found.":String.join("\n",issues),18,65);report.setEditable(false);report.setLineWrap(true);report.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this,new JScrollPane(report),"Map validation",issues.isEmpty()?JOptionPane.INFORMATION_MESSAGE:JOptionPane.WARNING_MESSAGE);
    }
    private void playtest(){
        sync();if(!renderer.editorState().world.isPassable("editor_workshop",document.spawnX,document.spawnY)){error(new IllegalArgumentException("Spawn is blocked. Use the Spawn tool to choose a walkable tile."));return;}
        MapDocument snapshot=document.copy();
        try{
            LoadingScreen.run(this,"Opening map playtest",progress -> GamePanel.prepare(root,progress),prepared -> {
                GamePanel panel=new GamePanel(root,true,prepared);JFrame game=new JFrame("Playtest - "+snapshot.label);
                try{
                    panel.startEditorPlaytest(snapshot);game.setDefaultCloseOperation(DISPOSE_ON_CLOSE);attachPlaytest(game,panel);
                    game.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){if(game.getContentPane() instanceof GamePanel active){active.shutdown();playtests.remove(active);}}});
                    game.pack();game.setLocationRelativeTo(this);game.setVisible(true);panel.requestFocusInWindow();
                }catch(RuntimeException ex){panel.shutdown();playtests.remove(panel);game.dispose();throw ex;}
            });
            info("Playtest opened with a detached copy. Close that window to return to authoring.");
        }catch(RuntimeException ex){error(ex);}
    }
    private void attachPlaytest(JFrame game,GamePanel panel){
        playtests.add(panel);game.setContentPane(panel);
        panel.setReplacementHandler(next -> {playtests.remove(panel);attachPlaytest(game,next);game.validate();});
    }
}
