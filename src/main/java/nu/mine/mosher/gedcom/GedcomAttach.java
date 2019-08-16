package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static nu.mine.mosher.gedcom.GedcomTag.*;

public class GedcomAttach {
    private static final Map<String, TreeNode<GedcomLine>> refnToNode = new HashMap<>(8192, 1.0F);
    private static final Map<String, String> idConvs = new HashMap<>();

    public static void main(final String... args) throws IOException, InvalidLevel {
        if (args.length < 2) {
            throw new IllegalArgumentException("\n\nUsage:\n    gedcom-attach OUTPUT.ged INPUT1.ged [ INPUT2.ged [ ... ] ]\n");
        }



        final GedcomTree treeOutput = GedcomMinimal.minimal(null);

        for (int i = 1; i < args.length; ++i) {
            attachInputFile(fileForPathArg(args[i]), treeOutput);
        }

        convertIds(treeOutput);

        writeTreeTo(treeOutput, fileForPathArg(args[0]));



        System.err.flush();
        System.out.flush();
    }

    private static void attachInputFile(final File fileInput, final GedcomTree treeOutput) throws IOException, InvalidLevel {
        final GedcomTree treeInput = readGedcom(fileInput);
        final TreeNode<GedcomLine> trlrOutput = getTrlr(treeOutput.getRoot());

        TreeNode<GedcomLine> top = treeInput.getRoot().getFirstChildOrNull();
        while (Objects.nonNull(top)) {
            top.removeFromParent();

            if (!filterDuplicateIndi(top)) {
                treeOutput.getRoot().addChildBefore(top, trlrOutput);
            }

            top = treeInput.getRoot().getFirstChildOrNull();
        }
    }

    private static final Set<GedcomTag> DROP = Set.of(HEAD, TRLR, SUBM, SUBN);
    private static boolean filterDuplicateIndi(final TreeNode<GedcomLine> top) {
        final GedcomLine item = top.getObject();
        final String id = item.getID();
        boolean drop = false;
        if (item.getTag().equals(INDI) && !id.isEmpty()) {
            final String refn = getRefnFrom(top);
            if (!refn.isEmpty()) {
                if (refnToNode.containsKey(refn)) {
                    // individual with duplicate reference number
                    // we need to remap their ID to the ID of the
                    // corresponding individual in the target file
                    idConvs.put(id, refnToNode.get(refn).getObject().getID());
                    copyFamcFams(top, refnToNode.get(refn));
                    // and drop this duplicate individual
                    drop = true;
                } else {
                    refnToNode.put(refn, top);
                }
            }
        } else if (DROP.contains(item.getTag())) {
            drop = true;
        }
        return drop;
    }

    private static void copyFamcFams(final TreeNode<GedcomLine> from, final TreeNode<GedcomLine> to) {
        for (final TreeNode<GedcomLine> a : from) {
            final GedcomLine attr = a.getObject();
            if (attr != null && (attr.getTag().equals(FAMC) || attr.getTag().equals(FAMS))) {
                to.addChild(new TreeNode<>(attr.replacePointer(attr.getPointer())));
            }
        }
    }

    private static String getRefnFrom(final TreeNode<GedcomLine> top) {
        for (final TreeNode<GedcomLine> node : top) {
            final GedcomLine attr = node.getObject();
            if (attr != null && attr.getTag().equals(REFN)) {
                return attr.getValue();
            }
        }
        return "";
    }

    private static void convertIds(GedcomTree treeOutput) {
        treeOutput.getRoot().forAll(node -> {
            final GedcomLine line = node.getObject();
            if (line != null && line.isLink() && idConvs.containsKey(line.getLink())) {
                node.setObject(line.replaceLink(idConvs.get(line.getLink())));
            }
        });
    }

    private static void writeTreeTo(final GedcomTree treeOutput, final File fileOutput) throws IOException {
        treeOutput.setMaxLength(60);
        new GedcomUnconcatenator(treeOutput).unconcatenate();
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileOutput));
        Gedcom.writeFile(treeOutput, out);
        out.flush();
        out.close();
    }

    private static GedcomTree readGedcom(File fileInput) throws IOException, InvalidLevel {
        final BufferedInputStream in = new BufferedInputStream(Files.newInputStream(fileInput.toPath()));
        final GedcomTree tree = Gedcom.readFile(in);
        in.close();
        new GedcomConcatenator(tree).concatenate();
        return tree;
    }

    private static File fileForPathArg(final String filepath) throws IOException {
        return Paths.get(filepath).toFile().getCanonicalFile().getAbsoluteFile();
    }

    private static TreeNode<GedcomLine> getTrlr(final TreeNode<GedcomLine> root) {
        for (final TreeNode<GedcomLine> top : root) {
            final GedcomLine gedcomLine = top.getObject();
            if (gedcomLine != null && gedcomLine.getTag().equals(GedcomTag.TRLR)) {
                return top;
            }
        }
        throw new IllegalStateException("Could not find TRLR in minimal GEDCOM file.");
    }
}
