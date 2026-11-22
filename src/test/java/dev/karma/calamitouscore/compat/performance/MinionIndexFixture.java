package dev.karma.calamitouscore.compat.performance;

import com.fevzi.summonity.entity.SummonedMinion;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.level.LevelEvent;

/** Constructor-free world tests; no actual game server is launched. */
final class MinionIndexFixture {
    static void run() throws Exception {
        // Only permit vanilla registry class initialization; do not run Forge's full launch bootstrap here.
        net.minecraft.SharedConstants.tryDetectVersion();
        var bootstrap=net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");bootstrap.setAccessible(true);bootstrap.setBoolean(null,true);
        Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)field.get(null);
        FakeLevel level=(FakeLevel)unsafe.allocateInstance(FakeLevel.class);level.entities=new LinkedHashMap<>();
        FakePlayer owner=(FakePlayer)unsafe.allocateInstance(FakePlayer.class);owner.uuid=new UUID(10,20);owner.setBoundingBox(new AABB(0,0,0,1,2,1));
        Random random=new Random(5);
        for(int i=0;i<100;i++){FakeMinion mob=(FakeMinion)unsafe.allocateInstance(FakeMinion.class);mob.number=i;mob.setId(i);mob.owner=i%3==0?new UUID(9,9):owner.uuid;mob.slots=i%4;int x=random.nextInt(400)-200;mob.setBoundingBox(new AABB(x,0,0,x+1,1,1));level.entities.put(i,mob);}
        var nativeLoader=new PerformanceCheck.Loader(false,true);var indexedLoader=new PerformanceCheck.Loader(true,true);
        var nativeCount=nativeLoader.loadClass("com.fevzi.summonity.entity.MinionHelper").getMethod("getUsedMinionSlots",ServerLevel.class,Player.class);
        var type=indexedLoader.loadClass("dev.karma.calamitouscore.compat.performance.MinionSlotIndex");var indexed=type.getMethod("used",ServerLevel.class,Player.class);
        same(nativeCount,indexed,level,owner);
        for(int i=0;i<100;i++){
            FakeMinion mob=(FakeMinion)level.entities.get(i);mob.slots=7;mob.owner=owner.uuid;mob.setBoundingBox(new AABB(i-50,0,0,i-49,1,1));same(nativeCount,indexed,level,owner);
        }
        FakeMinion addition=(FakeMinion)unsafe.allocateInstance(FakeMinion.class);addition.number=1000;addition.setId(1000);addition.owner=owner.uuid;addition.slots=2;addition.setBoundingBox(new AABB(1,0,0,2,1,1));
        type.getMethod("join",EntityJoinLevelEvent.class).invoke(null,new EntityJoinLevelEvent(addition,level));
        same(nativeCount,indexed,level,owner); // Join event precedes the engine making an entity accessible.
        level.entities.put(addition.number,addition);same(nativeCount,indexed,level,owner);
        addition.slots=0;same(nativeCount,indexed,level,owner);
        addition.owner=new UUID(4,8);same(nativeCount,indexed,level,owner);
        level.entities.remove(addition.number);type.getMethod("leave",EntityLeaveLevelEvent.class).invoke(null,new EntityLeaveLevelEvent(addition,level));same(nativeCount,indexed,level,owner);
        owner.setBoundingBox(new AABB(400,0,0,401,2,1));same(nativeCount,indexed,level,owner);
        type.getMethod("unload",LevelEvent.Unload.class).invoke(null,new LevelEvent.Unload(level));level.entities.clear();same(nativeCount,indexed,level,owner);
        owner.setBoundingBox(new AABB(0,0,0,1,2,1));
        for(int i=0;i<190;i++){
            Mob mob;
            if(i<10){FakeMinion minion=(FakeMinion)unsafe.allocateInstance(FakeMinion.class);minion.number=i;minion.owner=owner.uuid;minion.slots=1;mob=minion;}
            else mob=(PlainMob)unsafe.allocateInstance(PlainMob.class);
            mob.setId(i);mob.setBoundingBox(new AABB(i%30,0,0,i%30+1,1,1));level.entities.put(i,mob);
            type.getMethod("join",EntityJoinLevelEvent.class).invoke(null,new EntityJoinLevelEvent(mob,level));
        }
        same(nativeCount,indexed,level,owner);
        PerformanceCheck.bench("summon_slots_190_entities_10_minions",()->PerformanceCheck.blackhole=nativeCount.invoke(null,level,owner),()->PerformanceCheck.blackhole=indexed.invoke(null,level,owner),4000);
        System.out.println("PASS: actual indexed/native slot counts agree in constructor-free world fixtures: live cost/owner/range changes, staged spawn, despawn, owner movement and level unload.");
    }
    private static void same(java.lang.reflect.Method a,java.lang.reflect.Method b,ServerLevel l,Player p)throws Exception{Object before=a.invoke(null,l,p),after=b.invoke(null,l,p);PerformanceCheck.require(before.equals(after),"Indexed count differs from installed native query: "+before+" vs "+after);}
    public static class FakeLevel extends ServerLevel {
        Map<Integer,Entity> entities;
        private FakeLevel(){super(null,null,null,null,null,null,null,false,0,List.of(),false,null);}
        @Override public Iterable<Entity> getAllEntities(){return entities.values();}
        @Override public Entity getEntity(int id){return entities.get(id);}
        @Override public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type,AABB box,Predicate<? super T> filter){List<T> result=new ArrayList<>();for(Entity e:entities.values())if(type.isInstance(e)&&e.getBoundingBox().intersects(box)&&filter.test(type.cast(e)))result.add(type.cast(e));return result;}
    }
    public static class FakePlayer extends Player {
        UUID uuid;
        private FakePlayer(){super(null,null,0,null);}
        @Override public UUID getUUID(){return uuid;}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
    }
    public static class FakeMinion extends Mob implements SummonedMinion {
        UUID owner;int slots,number;
        private FakeMinion(){super(null,null);}
        @Override public int getId(){return number;}
        public void setOwner(Player player){owner=player.getUUID();}
        public UUID summonity$getOwnerId(){return owner;}
        public void summonity$setBaseDamage(double amount){}
        public void setCommandedTarget(LivingEntity target){}
        public LivingEntity getCommandedTarget(){return null;}
        public void clearFocus(){}
        public int summonity$getSlotCost(){return slots;}
        public void m_146870_(){}
    }
    public static class PlainMob extends Mob {private PlainMob(){super(null,null);}}
}
