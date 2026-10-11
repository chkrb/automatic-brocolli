// Based on https://github.com/touying-typ/touying/blob/main/themes/aqua.typ

#import "@preview/touying:0.8.0": *

#let slide(
  config: (:),
  repeat: auto,
  setting: body => body,
  composer: auto,
  ..bodies,
) = touying-slide-wrapper(self => {
  let header(self) = {
    place(left, dy: 45%, dx: 2%, image("./assets/code-clan.svg", height: 140%))
    place(center, dy: 50%, heading(utils.call-or-display(
      self,
      self.store.header,
    )))
    place(right, dy: 45%, dx: -2%, image("./assets/sih2026.png", height: 140%))
  }
  let footer(self) = {
    set text(size: 0.8em, stroke: none, fill: white)
    place(
      bottom,
      rect(
        width: 100%,
        height: 2.5em,
        fill: self.colors.primary-light,
      ),
    )
    place(center, `SIH Idea Submission`)
    place(right, dx: -5%, utils.call-or-display(self, self.store.footer))
  }
  let self = utils.merge-dicts(
    self,
    config-page(
      header: header,
      footer: footer,
    ),
  )
  touying-slide(
    self: self,
    config: config,
    repeat: repeat,
    setting: setting,
    composer: composer,
    ..bodies,
  )
})

#let idea-theme(
  aspect-ratio: "16-9",
  header: self => utils.display-current-heading(depth: self.slide-level),
  footer: context utils.slide-counter.display(),
  ..args,
  body,
) = {
  set text(size: 18pt)

  show: touying-slides.with(
    config-page(
      ..utils.page-args-from-aspect-ratio(aspect-ratio),
      margin: (x: 2em, top: 4em, bottom: 2em),
    ),
    config-common(slide-fn: slide),
    config-methods(
      init: (self: none, body) => {
        show heading: set text(fill: self.colors.primary-light)
        body
      },
      alert: utils.alert-with-primary-color,
    ),
    config-colors(
      primary: rgb("#003F88"),
      primary-light: rgb("#2159A5"),
      primary-lightest: rgb("#F2F4F8"),
      neutral-lightest: rgb("#FFFFFF"),
    ),
    // save the variables for later use
    config-store(
      align: align,
      header: header,
      footer: footer,
    ),
    ..args,
  )

  body
}
