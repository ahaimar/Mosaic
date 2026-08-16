package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.Interpolation;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.PlayerProgress;
import com.packs.mosaic.world.econ.EconomySimulation;
import com.packs.mosaic.world.econ.Investment;
import com.packs.mosaic.world.econ.InvestmentCatalog;
import com.packs.mosaic.world.econ.Resource;

/**
 * Full-width HUD strip replacing the old single status label (Task 19 UI
 * refactor). Every economy category is its own container chip — Main,
 * Energy, Transport, Wood, Tools, Furniture, Logistics, Operations — so
 * values are grouped, evenly spaced and never overlap. The strip sits in a
 * horizontal ScrollPane so it degrades gracefully on narrow windows.
 */
public class ResourceBar extends Table {

    private static final Color CHIP_CAPTION = new Color(0.68f, 0.68f, 0.90f, 1f);
    private static final Color STAR_COLOR   = new Color(1f, 0.82f, 0.35f, 1f);

    private final EconomySimulation economy;
    private final PlayerProgress progress;

    private final Label starsLabel;
    private final Chip main;
    private final Chip energy;
    private final Chip transport;
    private final Chip wood;
    private final Chip tools;
    private final Chip furniture;
    private final Chip logistics;
    private final Chip operations;

    public ResourceBar(Skin skin, EconomySimulation economy, PlayerProgress progress) {
        super(skin);
        this.economy = economy;
        this.progress = progress;

        setBackground(skin.getDrawable("panel"));
        pad(6f, 10f, 4f, 10f);

        main = new Chip(skin, "hud.resource.main");
        starsLabel = valueLabel(skin);
        starsLabel.setColor(STAR_COLOR);
        main.setRight(starsLabel);

        energy = new Chip(skin, "hud.resource.energy");
        transport = new Chip(skin, "hud.resource.transport");
        wood = new Chip(skin, "hud.resource.wood");
        tools = new Chip(skin, "hud.resource.tools");
        furniture = new Chip(skin, "hud.resource.furniture");
        logistics = new Chip(skin, "hud.resource.logistics");
        operations = new Chip(skin, "hud.resource.operations");

        Table chips = new Table(skin);
        chips.left();
        chips.add(main).padRight(6f);
        chips.add(energy).padRight(6f);
        chips.add(transport).padRight(6f);
        chips.add(wood).padRight(6f);
        chips.add(tools).padRight(6f);
        chips.add(furniture).padRight(6f);
        chips.add(logistics).padRight(6f);
        chips.add(operations);

        ScrollPane scroll = new ScrollPane(chips, skin);
        scroll.setScrollingDisabled(false, true);
        scroll.setOverscroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsOnTop(true);

        add(scroll).growX().row();
        Image divider = new Image(skin.getDrawable("divider"));
        add(divider).growX().height(2f).padTop(6f);

        refresh();
    }

    /** Repaints every container from the current economy state. */
    public void refresh() {
        EconomySimulation.CostLedger ledger = economy.getCostLedger();

        starsLabel.setText(String.valueOf(progress.getTotalStars()));

        main.setLine1(tr("econ.money") + " " + fmt0(economy.getMoney())
            + "  ·  " + tr("econ.population") + " " + fmt0(economy.getPopulation()));
        main.setLine2(tr("econ.workers") + " " + fmt0(economy.getEmployedWorkers())
            + "/" + fmt0(economy.getWorkingPopulation())
            + "  ·  " + tr("econ.wage") + " " + fmt2(economy.getAverageWage()));
        main.setLine3(tr("hud.tech") + " " + economy.getTechLevel());

        energy.setLine1(tr("hud.resource.prod") + " " + fmt0(economy.getEnergyProduction())
            + "  ·  " + tr("hud.resource.cons") + " " + fmt0(economy.getEnergyConsumption()));
        energy.setLine2(tr("hud.resource.bal") + " " + signed(economy.getEnergyBalance())
            + "  ·  " + tr("hud.resource.eff") + " " + (int) (economy.getEnergyEfficiency() * 100f) + "%");
        energy.setLine3(tr("hud.resource.storage") + " " + fmt1(economy.getStorageCosts()));

        transport.setLine1(tr("hud.logistics.trucks") + " " + economy.getTruckCount()
            + "  ·  " + tr("hud.logistics.routes") + " " + economy.getActiveRouteCount());
        transport.setLine2(tr("hud.logistics.transit") + " " + fmt1(economy.getTotalInTransit()));
        transport.setLine3(tr("hud.resource.cost") + " " + fmt1(economy.getTransportCosts()));

        wood.setLine1(goods(Resource.WOOD));
        wood.setLine2(tr("hud.resource.price") + " " + fmt2(economy.getPrice(Resource.WOOD)));
        wood.setLine3(tr("hud.resource.demand") + " " + fmt2(economy.getConsumerDemand(Resource.WOOD)));

        tools.setLine1(goods(Resource.TOOLS));
        tools.setLine2(tr("hud.resource.price") + " " + fmt2(economy.getPrice(Resource.TOOLS)));
        tools.setLine3(tr("hud.resource.demand") + " " + fmt2(economy.getConsumerDemand(Resource.TOOLS)));

        furniture.setLine1(goods(Resource.FURNITURE));
        furniture.setLine2(tr("hud.resource.price") + " " + fmt2(economy.getPrice(Resource.FURNITURE)));
        furniture.setLine3(tr("hud.resource.demand") + " " + fmt2(economy.getConsumerDemand(Resource.FURNITURE)));

        logistics.setLine1(tr("econ.maintenance") + " " + fmt1(ledger.maintenance)
            + "  ·  " + tr("econ.materials") + " " + fmt1(ledger.materials));
        logistics.setLine2(tr("econ.transport") + " " + fmt1(ledger.transport)
            + "  ·  " + tr("econ.hauling") + " " + fmt1(ledger.hauling));
        logistics.setLine3(tr("econ.revenue") + " " + signed(ledger.revenue)
            + "  ·  " + tr("econ.net") + " " + signed(ledger.net));

        operations.setLine1(researchLabel());
        operations.setLine2(investmentLabel());
        operations.setLine3(developmentLabel());
    }

    /** Pop animation for the star count (star gains). */
    public void bounceStars() {
        starsLabel.clearActions();
        starsLabel.setOrigin(Align.center);
        starsLabel.setScale(1f);
        starsLabel.addAction(Actions.sequence(
            Actions.scaleTo(1.6f, 1.6f, 0.1f, Interpolation.swingOut),
            Actions.scaleTo(1f, 1f, 0.2f, Interpolation.swingOut)));
    }

    private String goods(Resource resource) {
        return tr("hud.resource.stored") + " " + fmt0(economy.getInventory(resource))
            + "/" + fmt0(resource.getStorageLimit());
    }

    private String researchLabel() {
        if (economy.isMaxTechnology()) return tr("hud.tech.max");
        if (economy.isResearching()) {
            return tr("hud.tech.research") + " " + (int) (economy.getResearchProgress() * 100f) + "%";
        }
        return tr("hud.tech.ready");
    }

    private String investmentLabel() {
        if (!economy.isInvesting()) return tr("hud.invest.ready");
        Investment investment = InvestmentCatalog.get(economy.getActiveInvestmentId());
        String name = investment == null
            ? economy.getActiveInvestmentId()
            : tr(investment.getNameKey());
        return name + "  ·  " + (int) (economy.getInvestmentProgress() * 100f) + "%";
    }

    private String developmentLabel() {
        String name = tr(economy.getDevelopmentNameKey());
        if (economy.isMaxDevelopment()) return name + "  ·  " + tr("hud.development.max");
        return name + "  ·  " + (int) (economy.getDevelopmentProgress() * 100f) + "%";
    }

    private static String tr(String key) {
        return LocalizationManager.tr(key);
    }

    private static String fmt0(float value) {
        return String.format("%.0f", value);
    }

    private static String fmt1(float value) {
        return String.format("%.1f", value);
    }

    private static String fmt2(float value) {
        return String.format("%.2f", value);
    }

    private static String signed(float value) {
        return String.format("%+.1f", value);
    }

    private static Label valueLabel(Skin skin) {
        Label label = new Label("", skin, "small");
        label.setFontScale(0.72f);
        label.setColor(Color.WHITE);
        return label;
    }

    /** One grouped category container: caption row + three value lines. */
    private static class Chip extends Table {

        private final Label caption;
        private final Label line1;
        private final Label line2;
        private final Label line3;

        Chip(Skin skin, String captionKey) {
            setBackground(skin.getDrawable("chip"));
            pad(5f, 10f, 6f, 10f);

            caption = new Label(LocalizationManager.tr(captionKey).toUpperCase(), skin, "small");
            caption.setFontScale(0.6f);
            caption.setColor(CHIP_CAPTION);
            line1 = valueLabel(skin);
            line2 = valueLabel(skin);
            line3 = valueLabel(skin);

            add(caption).left().row();
            add(line1).left().padTop(2f).row();
            add(line2).left().padTop(1f).row();
            add(line3).left().padTop(1f);
        }

        void setRight(Label right) {
            Table captionRow = new Table();
            captionRow.add(caption).left().expandX();
            captionRow.add(right).right().padLeft(8f);
            clearChildren();
            add(captionRow).growX().row();
            add(line1).left().padTop(2f).row();
            add(line2).left().padTop(1f).row();
            add(line3).left().padTop(1f);
        }

        void setLine1(String text) {
            line1.setText(text);
        }

        void setLine2(String text) {
            line2.setText(text);
        }

        void setLine3(String text) {
            line3.setText(text);
        }
    }
}
