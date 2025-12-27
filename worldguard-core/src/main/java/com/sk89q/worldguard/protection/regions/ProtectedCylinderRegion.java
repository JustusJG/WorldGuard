/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.protection.regions;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldguard.util.MathUtils;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a cuboid region that can be protected.
 *
 * @author Grandpie (copied from ProtectedCuboidRegion)
 */
public class ProtectedCylinderRegion extends ProtectedRegion {

    private final int minY;
    private final int maxY;
    private final Vector2 radius;
    private final Vector2 center;

    public Vector2 getRadius() {
        return radius;
    }
    public Vector2 getCenter() {
        return center;
    }

    /**
     * Construct a new instance of this cylinder region.<br>
     * Equivalent to {@link #ProtectedCylinderRegion(String, boolean, int, int, Vector2, Vector2)
     * ProtectedCylinderRegion(id, false, maxY, minY, radius, center)}<br>
     * <code>transientRegion</code> will be set to false, and this region can be saved.
     *
     * @param id the region id
     * @param maxY maximum Y
     * @param minY minimum Y
     * @param radius radius along X and Z
     * @param center center position
     */
    public ProtectedCylinderRegion(String id, int maxY, int minY, Vector2 radius, Vector2 center) {
        this(id, false, maxY, minY, radius, center);
    }

    /**
     * Construct a new instance of this cylinder region.
     *
     * @param id the region id
     * @param transientRegion whether this region should only be kept in memory and not be saved
     * @param maxY maximum Y
     * @param minY minimum Y
     * @param radius radius along X and Z
     * @param center center position
     */
    public ProtectedCylinderRegion(String id, boolean transientRegion, int maxY, int minY, Vector2 radius, Vector2 center) {
        super(id, transientRegion);

        this.maxY = maxY;
        this.minY = minY;
        this.radius = radius;
        this.center = center;

        List<BlockVector3> points = new ArrayList<>();
        points.add(BlockVector3.at(
                center.blockX() + radius.blockX(),
                maxY,
                center.blockZ() + radius.blockZ()));
        points.add(BlockVector3.at(
                center.blockX() - radius.blockX(),
                minY,
                center.blockZ() - radius.blockZ()));
        setMinMaxPoints(points);
    }

    @Override
    public boolean isPhysicalArea() {
        return true;
    }

    @Override
    public List<BlockVector2> getPoints() {
        List<BlockVector2> pts = new ArrayList<>();
        int x1 = min.x();
        int x2 = max.x();
        int z1 = min.z();
        int z2 = max.z();

        pts.add(BlockVector2.at(x1, z1));
        pts.add(BlockVector2.at(x2, z1));
        pts.add(BlockVector2.at(x2, z2));
        pts.add(BlockVector2.at(x1, z2));

        return pts;
    }

    @Override
    public boolean contains(BlockVector3 pt) {
        if (pt.y() > maxY || pt.y() < minY)
            return false;

        double dx = pt.x() - center.x();
        double dz = pt.z() - center.z();
        final double normalizedX = dx / radius.x();
        final double normalizedZ = dz / radius.z();
        return (normalizedX * normalizedX + normalizedZ * normalizedZ) <= 1.0;
    }

    @Override
    public RegionType getType() {
        return RegionType.CYLINDER;
    }

    @Override
    Area toArea() {
        return new Area(new Ellipse2D.Double(
                center.x() - radius.x(),
                center.z() - radius.z(),
                2 * radius.x(),
                2 * radius.z()));
    }

    @Override
    protected boolean intersects(ProtectedRegion region, Area thisArea) {
        if (region instanceof ProtectedCylinderRegion) {
            return intersectsBoundingBox(region);
        } else {
            return super.intersects(region, thisArea);
        }
    }

    @Override
    public int volume() {
        // approximate, it's not block perfect
        try {
            int height = maxY - minY + 1;
            long area = MathUtils.checkedMultiply((long) radius.x(), (long) radius.z());
            long volume = MathUtils.checkedMultiply(area, height);

            if (volume > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else {
                return (int) (volume * Math.PI);
            }
        } catch (ArithmeticException e) {
            return Integer.MAX_VALUE;
        }
    }

}
