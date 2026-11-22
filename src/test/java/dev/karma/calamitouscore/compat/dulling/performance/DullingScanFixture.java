package dev.karma.calamitouscore.compat.dulling.performance;

import java.util.*;
import net.minecraft.core.BlockPos;

/** Exercises production primitive traversal with synthetic section palettes; no world or GL context. */
final class DullingScanFixture {
    static void run(ClassLoader unused) {
        for (int seed = 0; seed < 40; seed++) {
            Random random = new Random(seed);
            int centerX = seed - 20, centerY = seed * 3 - 64, centerZ = 40 - seed;
            BlockPos min = new BlockPos(centerX-12, centerY-12, centerZ-12), max = new BlockPos(centerX+12, centerY+12, centerZ+12);
            Map<BlockPos, Set<Integer>> sections = new HashMap<>(); Set<BlockPos> portals = new HashSet<>();
            for (int i = 0; i < 24; i++) {
                BlockPos p = new BlockPos(centerX+random.nextInt(31)-15,centerY+random.nextInt(31)-15,centerZ+random.nextInt(31)-15);
                portals.add(p); sections.computeIfAbsent(new BlockPos(p.getX()>>4,p.getY()>>4,p.getZ()>>4), ignored->new HashSet<>()).add((p.getY()&15)*256+(p.getZ()&15)*16+(p.getX()&15));
            }
            SparsePortalTraversal.Sections source = (x,y,z) -> {
                Set<Integer> states = sections.get(new BlockPos(x,y,z));
                return states == null ? null : (lx,ly,lz) -> states.contains(ly*256+lz*16+lx);
            };
            List<BlockPos> expected=new ArrayList<>(); for(BlockPos p:BlockPos.betweenClosed(min,max)) if(portals.contains(p)) expected.add(p.immutable());
            List<BlockPos> actual=SparsePortalTraversal.collect(source,min.getX(),min.getY(),min.getZ(),max.getX(),max.getY(),max.getZ());
            DullingPerfCheck.require(actual.equals(expected), "Position/order/negative section boundary mismatch "+seed);
            sections.clear();
            DullingPerfCheck.require(SparsePortalTraversal.collect(source,min.getX(),min.getY(),min.getZ(),max.getX(),max.getY(),max.getZ()).isEmpty(), "Changed/unloaded section cached across scans");
        }
        int[] probes={0};
        var empty=SparsePortalTraversal.collect((x,y,z)->{probes[0]++;return null;},-12,-12,-12,12,12,12);
        DullingPerfCheck.require(empty.isEmpty() && probes[0]==8,"Empty region did not use exactly 8 palette checks");
        System.out.println("PASS: production sparse traversal matches native position order across 40 synthetic regions, negative/chunk/section boundaries and changed/unloaded sections; empty 25-cube uses 8 section probes / 0 block reads vs 15,625 native position reads.");
    }
}
